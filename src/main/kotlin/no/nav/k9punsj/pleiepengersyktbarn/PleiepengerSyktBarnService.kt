package no.nav.k9punsj.pleiepengersyktbarn

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.dto.JournalposterDto
import no.nav.k9punsj.felles.dto.Matchfagsak
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.felles.dto.SøknadFeil
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.utils.ServerRequestUtils.søknadLocationUri
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json
import java.net.URI

@Service
internal class PleiepengerSyktBarnService(
    private val personService: PersonService,
    private val mappeService: MappeService,
    private val journalpostService: JournalpostService,
    private val azureGraphService: IAzureGraphService,
    private val objectMapper: ObjectMapper,
    private val soknadService: SoknadService,
    private val k9SakService: K9SakService,
    private val aksjonspunktService: AksjonspunktService
) {

    private companion object {
        val logger = LoggerFactory.getLogger(PleiepengerSyktBarnService::class.java)
    }

    internal suspend fun henteMappe(norskIdent: String): ServerResponse {
        val person = personService.finnPersonVedNorskIdent(norskIdent)
        if (person != null) {
            val svarDto = mappeService.hentMappe(
                person = person
            ).tilPsbVisning(norskIdent)
            return ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(svarDto)
        }
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(SvarPsbDto(norskIdent, PunsjFagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf()))
    }

    internal suspend fun henteSøknad(søknadId: String): ServerResponse {
        val søknad = soknadService.hentSøknad(søknadId)
            ?: return ServerResponse
                .notFound()
                .buildAndAwait()

        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad.tilPsbvisning())
    }

    internal suspend fun oppdaterEksisterendeSøknad(
        request: ServerRequest,
        søknad: PleiepengerSyktBarnSøknadDto
    ): ServerResponse {
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

        val søknadEntitet = mappeService.utfyllendeInnsendingPsb(
            pleiepengerSøknadDto = søknad,
            saksbehandler = saksbehandler
        ) ?: return ServerResponse.badRequest().buildAndAwait()

        val (entitet, _) = søknadEntitet
        val søker = personService.finnPerson(entitet.søkerId)
        journalpostService.settKildeHvisIkkeFinnesFraFør(
            hentUtJournalposter(entitet),
            søker.aktørId
        )

        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad)
    }

    internal suspend fun sendEksisterendeSøknad(sendSøknad: SendSøknad): ServerResponse {
        val søknadEntitet = soknadService.hentSøknad(sendSøknad.soeknadId)
            ?: throw innsendingErrorResponseException(
                status = HttpStatus.BAD_REQUEST,
                detail = "Søknad med id ${sendSøknad.soeknadId} finnes ikke."
            )

        try {
            val søknad: PleiepengerSyktBarnSøknadDto = objectMapper.convertValue(søknadEntitet.søknad!!)
            val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(søknadEntitet.k9saksnummer).first ?: emptyList()

            val journalPoster = søknadEntitet.journalposter!!
            val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)
            val journalpostIder = journalposterDto.journalposter.filter { journalpostId ->
                journalpostService.kanSendeInn(listOf(journalpostId)).also { kanSendesInn ->
                    if (!kanSendesInn) {
                        logger.warn("JournalpostId $journalpostId kan ikke sendes inn. Filtreres bort fra innsendingen.")
                    }
                }
            }.toMutableSet()

            if (journalpostIder.isEmpty()) {
                logger.error("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
                throw innsendingErrorResponseException(
                    status = HttpStatus.CONFLICT,
                    detail = "Innsendingen må inneholde minst en journalpost som kan sendes inn."
                )
            }

            val (søknadK9Format, feilISøknaden) = MapPsbTilK9Format(
                søknadId = søknad.soeknadId,
                journalpostIder = journalpostIder,
                perioderSomFinnesIK9 = hentPerioderSomFinnesIK9,
                dto = søknad
            ).søknadOgFeil()

            if (feilISøknaden.isNotEmpty()) {
                val feil = feilISøknaden.map { feil ->
                    SøknadFeil.SøknadFeilDto(
                        feil.felt,
                        feil.feilkode,
                        feil.feilmelding
                    )
                }.toList()

                throw innsendingValidationErrorResponseException(
                    søknadId = søknad.soeknadId,
                    feil = feil
                )
            }

            val feil = soknadService.opprettSakOgSendInnSøknad(
                søknad = søknadK9Format,
                brevkode = Brevkode.PLEIEPENGER_BARN_SOKNAD,
                journalpostIder = journalpostIder
            )
            if (feil != null) {
                val (httpStatus, feilen) = feil
                throw innsendingErrorResponseException(
                    status = httpStatus,
                    detail = feilen
                )
            }

            val ansvarligSaksbehandler = soknadService.hentSistEndretAvSaksbehandler(søknad.soeknadId)
            aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(
                journalpostId = journalpostIder,
                erSendtInn = true,
                ansvarligSaksbehandler = ansvarligSaksbehandler
            )

            return ServerResponse
                .accepted()
                .json()
                .bodyValueAndAwait(søknadK9Format)
        } catch (error: Throwable) {
            if (error is ErrorResponseException && error.statusCode.value() != HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                throw error
            }
            logger.error(error.localizedMessage, error)
            throw error.asInnsendingErrorResponseException()
        }
    }

    internal suspend fun nySøknad(request: ServerRequest, nySøknad: OpprettNySøknad): ServerResponse {
        // setter riktig type der man jobber på en ukjent i utgangspunktet
        journalpostService.settFagsakYtelseType(
            ytelseType = PunsjFagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            journalpostId = nySøknad.journalpostId
        )

        val søknadEntitet = mappeService.førsteInnsendingPsb(
            nySøknad = nySøknad
        )
        return ServerResponse
            .created(request.søknadLocationUri(søknadEntitet.søknadId))
            .json()
            .bodyValueAndAwait(søknadEntitet.tilPsbvisning())
    }

    internal suspend fun validerSøknad(soknadTilValidering: PleiepengerSyktBarnSøknadDto): ServerResponse {
        val søknadEntitet = soknadService.hentSøknad(soknadTilValidering.soeknadId)
            ?: throw valideringErrorResponseException(
                status = HttpStatus.BAD_REQUEST,
                detail = "Søknad med id ${soknadTilValidering.soeknadId} finnes ikke."
            )

        val (søknad, feilListe) = try {
            val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(søknadEntitet.k9saksnummer).first ?: emptyList()
            val journalPoster = søknadEntitet.journalposter!!
            val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)
            MapPsbTilK9Format(
                søknadId = soknadTilValidering.soeknadId,
                journalpostIder = journalposterDto.journalposter,
                perioderSomFinnesIK9 = hentPerioderSomFinnesIK9,
                dto = soknadTilValidering
            ).søknadOgFeil()
        } catch (error: Throwable) {
            logger.error("Uventet feil under validering av PSB søknad", error)
            throw valideringErrorResponseException(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                detail = normalizedErrorMessage(error),
                cause = error
            )
        }

        if (feilListe.isNotEmpty()) {
            val feil = feilListe.map { feil ->
                SøknadFeil.SøknadFeilDto(
                    feil.felt,
                    feil.feilkode,
                    feil.feilmelding
                )
            }.toList()

            throw valideringValidationErrorResponseException(
                søknadId = soknadTilValidering.soeknadId,
                feil = feil
            )
        }
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
        mappeService.utfyllendeInnsendingPsb(
            pleiepengerSøknadDto = soknadTilValidering,
            saksbehandler = saksbehandler
        )
        return ServerResponse
            .status(HttpStatus.ACCEPTED)
            .json()
            .bodyValueAndAwait(søknad)
    }

    @Deprecated("Flyttes til felles k9-sak tjeneste")
    internal suspend fun hentInfoFraK9Sak(matchfagsak: Matchfagsak): ServerResponse {
        val (perioder, _) = k9SakService.hentPerioderSomFinnesIK9(
            matchfagsak.brukerIdent,
            matchfagsak.barnIdent,
            PunsjFagsakYtelseType.PLEIEPENGER_SYKT_BARN
        )

        return if (perioder != null) {
            ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(perioder)
        } else {
            ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(listOf<PeriodeDto>())
        }
    }

    private suspend fun henterPerioderSomFinnesIK9sak(saksnummer: String?): Pair<List<PeriodeDto>?, String?> {
        if (saksnummer.isNullOrBlank()) {
            return Pair(emptyList(), null)
        }
        return k9SakService.hentPerioderSomFinnesIK9ForSaksnummer(saksnummer)
    }

    private fun Throwable.asInnsendingErrorResponseException(): ErrorResponseException {
        val detail = normalizedErrorMessage(this)
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detail).apply {
            title = "Feil ved innsending av søknad"
            type = URI("/problem-details/innsending-feil")
        }
        return ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail, this)
    }

    private fun innsendingErrorResponseException(
        status: HttpStatus,
        detail: String?,
        properties: Map<String, Any?> = emptyMap()
    ): ErrorResponseException {
        val normalizedDetail = detail?.trim()?.takeIf { it.isNotEmpty() } ?: "Uhåndtert feil uten detaljer"
        val (title, type) = when (status) {
            HttpStatus.BAD_REQUEST -> "Ugyldig søknad for innsending" to URI("/problem-details/innsending-validering-feil")
            HttpStatus.CONFLICT -> "Konflikt ved innsending av søknad" to URI("/problem-details/innsending-konflikt")
            else -> "Feil ved innsending av søknad" to URI("/problem-details/innsending-feil")
        }

        val problemDetail = ProblemDetail.forStatusAndDetail(status, normalizedDetail).apply {
            this.title = title
            this.type = type
            properties
                .filterValues { it != null }
                .forEach { (key, value) ->
                    setProperty(key, value)
                }
        }
        return ErrorResponseException(status, problemDetail, null)
    }

    private fun innsendingValidationErrorResponseException(
        søknadId: String,
        feil: List<SøknadFeil.SøknadFeilDto>
    ): ErrorResponseException {
        return innsendingErrorResponseException(
            status = HttpStatus.BAD_REQUEST,
            detail = "Søknaden inneholder valideringsfeil",
            properties = mapOf(
                "soeknadId" to søknadId,
                "feil" to feil
            )
        )
    }

    private fun valideringErrorResponseException(
        status: HttpStatus,
        detail: String?,
        properties: Map<String, Any?> = emptyMap(),
        cause: Throwable? = null
    ): ErrorResponseException {
        val normalizedDetail = detail?.trim()?.takeIf { it.isNotEmpty() } ?: "Uhåndtert feil uten detaljer"
        val (title, type) = when (status) {
            HttpStatus.BAD_REQUEST -> "Ugyldig søknad for validering" to URI("/problem-details/validering-feil")
            else -> "Feil ved validering av søknad" to URI("/problem-details/validering-uventet-feil")
        }

        val problemDetail = ProblemDetail.forStatusAndDetail(status, normalizedDetail).apply {
            this.title = title
            this.type = type
            properties
                .filterValues { it != null }
                .forEach { (key, value) ->
                    setProperty(key, value)
                }
        }
        return ErrorResponseException(status, problemDetail, cause)
    }

    private fun valideringValidationErrorResponseException(
        søknadId: String,
        feil: List<SøknadFeil.SøknadFeilDto>
    ): ErrorResponseException {
        return valideringErrorResponseException(
            status = HttpStatus.BAD_REQUEST,
            detail = "Søknaden inneholder valideringsfeil",
            properties = mapOf(
                "soeknadId" to søknadId,
                "feil" to feil
            )
        )
    }

    private fun normalizedErrorMessage(error: Throwable): String {
        val messageCandidates = listOfNotNull(
            (error as? ErrorResponseException)?.body?.detail,
            error.message,
            error.localizedMessage
        ).map { it.trim() }.filter { it.isNotEmpty() }

        messageCandidates.forEach { candidate ->
            extractFeilmelding(candidate)?.let { return it }
        }

        return "Uhåndtert feil uten detaljer"
    }

    private fun extractFeilmelding(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return null
        }

        parseFeilmeldingFromJson(trimmed)?.let { return it }

        val detailPattern = Regex(
            pattern = "detail='(.*?)'(?=,\\s*instance=|,\\s*properties=|\\])",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )
        val detailText = detailPattern.find(trimmed)?.groupValues?.get(1)?.trim()
        if (!detailText.isNullOrBlank()) {
            parseFeilmeldingFromJson(detailText)?.let { return it }
            return detailText
        }

        return trimmed
    }

    private fun parseFeilmeldingFromJson(raw: String): String? {
        val sanitized = raw.trim()
        val candidates = buildList {
            add(sanitized)
            add(sanitized.replace("\\\"", "\""))
            if (sanitized.startsWith("\"") && sanitized.endsWith("\"") && sanitized.length > 1) {
                add(sanitized.substring(1, sanitized.length - 1))
            }
        }.distinct()

        candidates.forEach { candidate ->
            val jsonNode = runCatching { objectMapper.readTree(candidate) }.getOrNull() ?: return@forEach
            findFeilmelding(jsonNode)?.let { return it }
        }

        return null
    }

    private fun findFeilmelding(node: JsonNode): String? {
        node.get("feilmelding")
            ?.takeIf { it.isTextual }
            ?.asText()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val detailNode = node.get("detail") ?: return null
        return if (detailNode.isTextual) {
            extractFeilmelding(detailNode.asText())
        } else {
            findFeilmelding(detailNode)
        }
    }
}
