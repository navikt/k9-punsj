package no.nav.k9punsj.opplaeringspenger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
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
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.utils.ServerRequestUtils.søknadLocationUri
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json

@Service
internal class OpplaeringspengerService(
    private val personService: PersonService,
    private val mappeService: MappeService,
    private val journalpostService: JournalpostService,
    private val azureGraphService: IAzureGraphService,
    private val objectMapper: ObjectMapper,
    private val soknadService: SoknadService,
    private val aksjonspunktService: AksjonspunktService,
    private val k9SakService: K9SakService
) {

    private companion object {
        val logger = LoggerFactory.getLogger(OpplaeringspengerService::class.java)
    }

    internal suspend fun henteMappe(norskIdent: String): ServerResponse {
        val person = personService.finnPersonVedNorskIdent(norskIdent)
        if (person != null) {
            val svarDto = mappeService.hentMappe(
                person = person
            ).tilOlpVisning(norskIdent)
            return ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(svarDto)
        }
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(SvarOlpDto(norskIdent, PunsjFagsakYtelseType.OPPLÆRINGSPENGER.kode, listOf()))
    }

    internal suspend fun henteSøknad(søknadId: String): ServerResponse {
        val søknad = soknadService.hentSøknad(søknadId)
            ?: return ServerResponse
                .notFound()
                .buildAndAwait()

        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad.tilOlpvisning())
    }

    internal suspend fun hentInstitusjoner(): ServerResponse {
        val institusjoner = k9SakService.hentInstitusjoner()
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(institusjoner)
    }

    internal suspend fun oppdaterEksisterendeSøknad(
        request: ServerRequest,
        søknad: OpplaeringspengerSøknadDto
    ): ServerResponse {
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

        val søknadEntitet = mappeService.utfyllendeInnsendingOlp(
            opplaeringspengerSøknadDto = søknad,
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
            ?: return ServerResponse.badRequest().buildAndAwait()

        try {
            val søknad: OpplaeringspengerSøknadDto = objectMapper.convertValue(søknadEntitet.søknad!!)
            val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(søknad)?.first ?: emptyList()

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
                return ServerResponse
                    .status(HttpStatus.CONFLICT)
                    .bodyValueAndAwait(OasFeil("Innsendingen må inneholde minst en journalpost som kan sendes inn."))
            }

            val (søknadK9Format, feilISøknaden) = MapOlpTilK9Format(
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

                return ServerResponse
                    .status(HttpStatus.BAD_REQUEST)
                    .json()
                    .bodyValueAndAwait(SøknadFeil(søknad.soeknadId, feil))
            }

            val feil = soknadService.opprettSakOgSendInnSøknad(
                søknad = søknadK9Format,
                brevkode = Brevkode.OPPLÆRINGSPENGER_SOKNAD,
                journalpostIder = journalpostIder
            )
            if (feil != null) {
                val (httpStatus, feilen) = feil

                return ServerResponse
                    .status(httpStatus)
                    .json()
                    .bodyValueAndAwait(OasFeil(feilen))
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
        } catch (e: Exception) {
            logger.error(e.localizedMessage, e)
            return ServerResponse
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .json()
                .bodyValueAndAwait(e.localizedMessage)
        }
    }

    internal suspend fun nySøknad(request: ServerRequest, nySøknad: OpprettNySøknad): ServerResponse {
        // setter riktig type der man jobber på en ukjent i utgangspunktet
        journalpostService.settFagsakYtelseType(
            PunsjFagsakYtelseType.OPPLÆRINGSPENGER,
            nySøknad.journalpostId
        )

        val søknadEntitet = mappeService.førsteInnsendingOlp(
            nySøknad = nySøknad
        )
        return ServerResponse
            .created(request.søknadLocationUri(søknadEntitet.søknadId))
            .json()
            .bodyValueAndAwait(søknadEntitet.tilOlpvisning())
    }

    internal suspend fun validerSøknad(
        soknadTilValidering: OpplaeringspengerSøknadDto
    ): ServerResponse {
        val søknadEntitet = soknadService.hentSøknad(soknadTilValidering.soeknadId)
            ?: return ServerResponse
                .badRequest()
                .buildAndAwait()

        val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(soknadTilValidering)?.first ?: emptyList()
        val journalPoster = søknadEntitet.journalposter!!
        val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

        val mapTilEksternFormat: Pair<Søknad, List<Feil>>?

        try {
            mapTilEksternFormat = MapOlpTilK9Format(
                søknadId = soknadTilValidering.soeknadId,
                journalpostIder = journalposterDto.journalposter,
                perioderSomFinnesIK9 = hentPerioderSomFinnesIK9,
                dto = soknadTilValidering
            ).søknadOgFeil()
        } catch (e: Exception) {
            return ServerResponse
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .json()
                .bodyValueAndAwait(e.localizedMessage)
        }

        val (søknad, feilListe) = mapTilEksternFormat
        if (feilListe.isNotEmpty()) {
            val feil = feilListe.map { feil ->
                SøknadFeil.SøknadFeilDto(
                    feil.felt,
                    feil.feilkode,
                    feil.feilmelding
                )
            }.toList()

            return ServerResponse
                .status(HttpStatus.BAD_REQUEST)
                .json()
                .bodyValueAndAwait(SøknadFeil(soknadTilValidering.soeknadId, feil))
        }
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
        mappeService.utfyllendeInnsendingOlp(
            opplaeringspengerSøknadDto = soknadTilValidering,
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
            PunsjFagsakYtelseType.OPPLÆRINGSPENGER
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

    private suspend fun henterPerioderSomFinnesIK9sak(dto: OpplaeringspengerSøknadDto): Pair<List<PeriodeDto>?, String?>? {
        if (dto.soekerId.isNullOrBlank() || dto.barn == null || dto.barn.norskIdent.isNullOrBlank()) {
            return null
        }
        return k9SakService.hentPerioderSomFinnesIK9(
            søker = dto.soekerId,
            barn = dto.barn.norskIdent,
            punsjFagsakYtelseType = PunsjFagsakYtelseType.OPPLÆRINGSPENGER
        )
    }
}
