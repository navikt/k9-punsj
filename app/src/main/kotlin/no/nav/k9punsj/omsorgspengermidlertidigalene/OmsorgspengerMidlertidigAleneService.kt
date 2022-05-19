package no.nav.k9punsj.omsorgspengermidlertidigalene

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.felles.dto.JournalposterDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.felles.dto.SøknadFeil
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
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
class OmsorgspengerMidlertidigAleneService(
    private val personService: PersonService,
    private val mappeService: MappeService,
    private val punsjbolleService: PunsjbolleService,
    private val journalpostService: JournalpostService,
    private val azureGraphService: IAzureGraphService,
    private val soknadService: SoknadService,
    private val objectMapper: ObjectMapper
    ) {

    private companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerMidlertidigAleneService::class.java)
    }

    internal suspend fun henteMappe(norskIdent: String): ServerResponse {
        val person = personService.finnPersonVedNorskIdent(norskIdent)
        if (person != null) {
            val svarDto = mappeService.hentMappe(
                person = person
            ).tilOmsMAVisning(norskIdent)
            return ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(svarDto)
        }
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(
                SvarOmsMADto(
                    norskIdent,
                    FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE.kode,
                    listOf()
                )
            )
    }

    internal suspend fun henteSøknad(søknadId: String): ServerResponse {
        val søknad = mappeService.hentSøknad(søknad = søknadId)
            ?: return ServerResponse.notFound().buildAndAwait()

        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad.tilOmsMAvisning())
    }

    internal suspend fun nySøknad(request: ServerRequest, nyOmsMASøknad: NyOmsMASøknad): ServerResponse {
        //oppretter sak i k9-sak hvis det ikke finnes fra før
        punsjbolleService.opprettEllerHentFagsaksnummer(
            søker = nyOmsMASøknad.norskIdent,
            annenPart = nyOmsMASøknad.annenPart,
            journalpostId = nyOmsMASøknad.journalpostId,
            periode = null,
            fagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA
        )

        //setter riktig type der man jobber på en ukjent i utgangspunktet
        journalpostService.settFagsakYtelseType(
            ytelseType = FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE,
            journalpostId = nyOmsMASøknad.journalpostId
        )

        val søknadEntitet = mappeService.førsteInnsendingOmsMA(
            nySøknad = nyOmsMASøknad
        )
        return ServerResponse
            .created(request.søknadLocationUri(søknadEntitet.søknadId))
            .json()
            .bodyValueAndAwait(søknadEntitet.tilOmsMAvisning())
    }

    internal suspend fun oppdaterEksisterendeSøknad(søknad: OmsorgspengerMidlertidigAleneSøknadDto): ServerResponse {
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

        val søknadEntitet = mappeService.utfyllendeInnsendingOmsMA(
            omsorgspengerMidlertidigAleneSøknadDto = søknad,
            saksbehandler = saksbehandler
        ) ?: return ServerResponse.badRequest().buildAndAwait()

        val (entitet, _) = søknadEntitet
        val søker = personService.finnPerson(personId = entitet.søkerId)
        journalpostService.settKildeHvisIkkeFinnesFraFør(
            journalposter = hentUtJournalposter(entitet),
            aktørId = søker.aktørId
        )
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad)
    }

    internal suspend fun sendEksisterendeSøknad(sendSøknad: SendSøknad): ServerResponse {
        val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)
            ?: return ServerResponse.badRequest().buildAndAwait()

        try {
            val søknad: OmsorgspengerMidlertidigAleneSøknadDto = objectMapper.convertValue(søknadEntitet.søknad!!)

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

            val (søknadK9Format, feilListe) = MapOmsMATilK9Format(
                søknadId = søknad.soeknadId,
                journalpostIder = journalpostIder,
                dto = søknad
            ).søknadOgFeil()

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
                    .bodyValueAndAwait(SøknadFeil(sendSøknad.soeknadId, feil))
            }

            val feil = soknadService.sendSøknad(
                søknad = søknadK9Format,
                journalpostIder = journalpostIder
            )

            if (feil != null) {
                val (httpStatus, feilen) = feil
                return ServerResponse
                    .status(httpStatus)
                    .json()
                    .bodyValueAndAwait(OasFeil(feilen))
            }

            return ServerResponse
                .accepted()
                .json()
                .bodyValueAndAwait(søknadK9Format)

        } catch (e: Exception) {
            return ServerResponse
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .json()
                .bodyValueAndAwait(e.localizedMessage)
        }
    }

    internal suspend fun validerSøknad(soknadTilValidering: OmsorgspengerMidlertidigAleneSøknadDto): ServerResponse {
        val søknadEntitet = mappeService.hentSøknad(soknadTilValidering.soeknadId)
            ?: return ServerResponse
                .badRequest()
                .buildAndAwait()

        val journalPoster = søknadEntitet.journalposter!!
        val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

        val mapTilEksternFormat: Pair<Søknad, List<Feil>>?

        try {
            mapTilEksternFormat = MapOmsMATilK9Format(
                søknadId = soknadTilValidering.soeknadId,
                journalpostIder = journalposterDto.journalposter,
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
        return ServerResponse
            .status(HttpStatus.ACCEPTED)
            .json()
            .bodyValueAndAwait(søknad)
    }
}
