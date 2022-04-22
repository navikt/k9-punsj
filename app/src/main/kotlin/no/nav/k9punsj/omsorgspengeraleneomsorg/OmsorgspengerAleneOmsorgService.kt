package no.nav.k9punsj.omsorgspengeraleneomsorg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.felles.dto.JournalposterDto
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.domenetjenester.dto.SøknadFeil
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.journalpost.JournalpostRepository
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
internal class OmsorgspengerAleneOmsorgService(
    private val objectMapper: ObjectMapper,
    private val mappeService: MappeService,
    private val personService: PersonService,
    private val punsjbolleService: PunsjbolleService,
    private val azureGraphService: IAzureGraphService,
    private val journalpostRepository: JournalpostRepository,
    private val soknadService: SoknadService,
) {

    private val logger = LoggerFactory.getLogger(OmsorgspengerAleneOmsorgService::class.java)

    suspend fun henteMappe(norskIdent: String): ServerResponse {
        val person = personService.finnPersonVedNorskIdent(norskIdent)
        if (person != null) {
            val svarDto = mappeService.hentMappe(person = person)
                .tilOmsAOVisning(norskIdent)
            return ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(svarDto)
        }
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(
                SvarOmsAODto(
                    søker = norskIdent,
                    fagsakTypeKode = FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN.kode,
                    søknader = listOf()
                )
            )
    }

    suspend fun henteSøknad(søknadId: String): ServerResponse {
        val søknad = mappeService.hentSøknad(søknad = søknadId)
            ?: return ServerResponse.notFound().buildAndAwait()

        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad.tilOmsAOvisning())
    }

    suspend fun nySøknad(request: ServerRequest, nySøknad: OpprettNySøknad): ServerResponse {
        //oppretter sak i k9-sak hvis det ikke finnes fra før
        if (nySøknad.pleietrengendeIdent != null) {
            punsjbolleService.opprettEllerHentFagsaksnummer(
                søker = nySøknad.norskIdent,
                pleietrengende = nySøknad.pleietrengendeIdent,
                journalpostId = nySøknad.journalpostId,
                periode = null,
                fagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO
            )
        }

        //setter riktig type der man jobber på en ukjent i utgangspunktet
        journalpostRepository.settFagsakYtelseType(
            ytelseType = FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN,
            journalpostId = nySøknad.journalpostId
        )

        val søknadEntitet = mappeService.førsteInnsendingOmsAO(
            nySøknad = nySøknad
        )
        return ServerResponse
            // TODO: Erstatt denne for o bli kvitt ServerRequest i service. (samme i alla ytelser)
            .created(request.søknadLocationUri(søknadEntitet.søknadId))
            .json()
            .bodyValueAndAwait(søknadEntitet.tilOmsAOvisning())
    }

    suspend fun oppdaterEksisterendeSøknad(søknad: OmsorgspengerAleneOmsorgSøknadDto): ServerResponse {
        val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
        val søknadEntitet = mappeService.utfyllendeInnsendingOmsAO(
            dto = søknad,
            saksbehandler = saksbehandler
        ) ?: return ServerResponse.badRequest().buildAndAwait()

        val (entitet, _) = søknadEntitet
        val søker = personService.finnPerson(personId = entitet.søkerId)
        journalpostRepository.settKildeHvisIkkeFinnesFraFør(
            journalposter = hentUtJournalposter(entitet),
            aktørId = søker.aktørId
        )
        return ServerResponse
            .ok()
            .json()
            .bodyValueAndAwait(søknad)
    }

    suspend fun sendEksisterendeSøknad(sendSøknad: SendSøknad): ServerResponse {
        val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)
            ?: return ServerResponse.badRequest().buildAndAwait()

        try {
            val søknad: OmsorgspengerAleneOmsorgSøknadDto =
                objectMapper.convertValue(søknadEntitet.søknad!!)

            val journalPoster = søknadEntitet.journalposter!!
            val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)
            val journalpostIder = journalposterDto.journalposter.filter { journalpostId ->
                journalpostRepository.kanSendeInn(listOf(journalpostId)).also { kanSendesInn ->
                    if (!kanSendesInn) {
                        logger.warn("JournalpostId $journalpostId kan ikke sendes inn. Filtreres bort fra innsendingen.")
                    }
                }
            }.toMutableSet()

            if (journalpostIder.isEmpty()) {
                logger.error("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
                return ServerResponse
                    .status(HttpStatus.CONFLICT)
                    .bodyValueAndAwait("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
            }

            val (søknadK9Format, feilListe) = MapOmsAOTilK9Format(
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
                    .bodyValueAndAwait(feilen)
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

    suspend fun validerSøknad(soknadTilValidering: OmsorgspengerAleneOmsorgSøknadDto): ServerResponse {
        val søknadEntitet = mappeService.hentSøknad(soknadTilValidering.soeknadId)
            ?: return ServerResponse.badRequest().buildAndAwait()

        val journalPoster = søknadEntitet.journalposter!!
        val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

        val mapTilEksternFormat: Pair<Søknad, List<Feil>>?

        try {
            mapTilEksternFormat = MapOmsAOTilK9Format(
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