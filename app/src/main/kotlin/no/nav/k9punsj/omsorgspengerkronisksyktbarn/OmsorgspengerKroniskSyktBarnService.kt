package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.domenetjenester.dto.JournalposterDto
import no.nav.k9punsj.domenetjenester.dto.SøknadFeil
import no.nav.k9punsj.domenetjenester.dto.hentUtJournalposter
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.azuregraph.AzureGraphService
import no.nav.k9punsj.utils.ServerRequestUtils.mapNySøknad
import no.nav.k9punsj.utils.ServerRequestUtils.norskIdent
import no.nav.k9punsj.utils.ServerRequestUtils.sendSøknad
import no.nav.k9punsj.utils.ServerRequestUtils.søknadLocation
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Service
internal class OmsorgspengerKroniskSyktBarnService(
    private val objectMapper: ObjectMapper,
    private val personService: PersonService,
    private val mappeService: MappeService,
    private val punsjbolleService: PunsjbolleService,
    private val journalpostRepository: JournalpostRepository,
    private val azureGraphService: AzureGraphService,
    private val soknadService: SoknadService,
) {

    private val logger = LoggerFactory.getLogger(OmsorgspengerKroniskSyktBarnService::class.java)

    suspend fun henteMappe(request: ServerRequest): ServerResponse {
        return RequestContext(coroutineContext, request) {
            val norskIdent = request.norskIdent()
            val person = personService.finnPersonVedNorskIdent(norskIdent)

            if (person != null) {
                val svarDto = mappeService.hentMappe(
                    person = person
                ).tilOmsKSBVisning(norskIdent)
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(svarDto)
            }
            return@RequestContext ServerResponse
                .ok()
                .json()
                .bodyValueAndAwait(SvarOmsKSBDto(norskIdent, FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN.kode, listOf()))
        }
    }

    suspend fun henteSøknad(request: ServerRequest): ServerResponse {
        return RequestContext(coroutineContext, request) {
            val søknadId = request.pathVariable("soeknad_id")
            val søknad = mappeService.hentSøknad(søknadId)

            if (søknad != null) {
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(søknad.tilOmsKSBvisning())
            }
            return@RequestContext ServerResponse
                .notFound()
                .buildAndAwait()
        }
    }

    suspend fun nySøknad(request: ServerRequest): ServerResponse {
        return RequestContext(coroutineContext, request) {
            val nySøknad = request.mapNySøknad()

            //oppretter sak i k9-sak hvis det ikke finnes fra før
            if (nySøknad.pleietrengendeIdent != null) {
                punsjbolleService.opprettEllerHentFagsaksnummer(
                    søker = nySøknad.norskIdent,
                    pleietrengende = nySøknad.pleietrengendeIdent,
                    journalpostId = nySøknad.journalpostId,
                    periode = null,
                    correlationId = coroutineContext.hentCorrelationId(),
                    fagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS
                )
            }

            //setter riktig type der man jobber på en ukjent i utgangspunktet
            journalpostRepository.settFagsakYtelseType(FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN, nySøknad.journalpostId)

            val søknadEntitet = mappeService.førsteInnsendingOmsKSB(
                nySøknad = nySøknad!!
            )
            return@RequestContext ServerResponse
                .created(request.søknadLocation(søknadEntitet.søknadId))
                .json()
                .bodyValueAndAwait(søknadEntitet.tilOmsKSBvisning())
        }
    }

    suspend fun oppdaterEksisterendeSøknad(request: ServerRequest): ServerResponse {
        return RequestContext(coroutineContext, request) {
            val søknad = request.body(BodyExtractors.toMono(OmsorgspengerKroniskSyktBarnSøknadDto::class.java))
                .awaitFirst()

            val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

            val søknadEntitet = mappeService.utfyllendeInnsendingOmsKSB(
                omsorgspengerKroniskSyktBarnSøknadDto = søknad,
                saksbehandler = saksbehandler
            )

            if (søknadEntitet == null) {
                ServerResponse
                    .badRequest()
                    .buildAndAwait()
            } else {
                val (entitet, _) = søknadEntitet
                val søker = personService.finnPerson(entitet.søkerId)
                journalpostRepository.settKildeHvisIkkeFinnesFraFør(
                    hentUtJournalposter(entitet),
                    søker.aktørId)
                ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(søknad)
            }
        }
    }

    suspend fun sendEksisterendeSøknad(request: ServerRequest): ServerResponse {
        return RequestContext(coroutineContext, request) {
            val sendSøknad = request.sendSøknad()
            val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)

            if (søknadEntitet == null) {
                return@RequestContext ServerResponse
                    .badRequest()
                    .buildAndAwait()
            } else {
                try {
                    val søknad: OmsorgspengerKroniskSyktBarnSøknadDto = objectMapper.convertValue(søknadEntitet.søknad!!)

                    val journalPoster = søknadEntitet.journalposter!!
                    val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)
                    val journalpostIder = journalposterDto.journalposter.filter { journalpostId ->
                        journalpostRepository.kanSendeInn(listOf(journalpostId)).also { kanSendesInn -> if (!kanSendesInn) {
                            logger.warn("JournalpostId $journalpostId kan ikke sendes inn. Filtreres bort fra innsendingen.")
                        }}
                    }.toMutableSet()

                    if (journalpostIder.isEmpty()) {
                        logger.error("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
                        return@RequestContext ServerResponse
                            .status(HttpStatus.CONFLICT)
                            .bodyValueAndAwait(OasFeil("Innsendingen må inneholde minst en journalpost som kan sendes inn."))
                    }

                    val (søknadK9Format, feilListe) = MapOmsKSBTilK9Format(
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

                        return@RequestContext ServerResponse
                            .status(HttpStatus.BAD_REQUEST)
                            .json()
                            .bodyValueAndAwait(SøknadFeil(sendSøknad.soeknadId, feil))
                    }

                    val feil = soknadService.sendSøknad(
                        søknadK9Format,
                        journalpostIder
                    )

                    if (feil != null) {
                        val (httpStatus, feilen) = feil
                        return@RequestContext ServerResponse
                            .status(httpStatus)
                            .json()
                            .bodyValueAndAwait(feilen)
                    }

                    return@RequestContext ServerResponse
                        .accepted()
                        .json()
                        .bodyValueAndAwait(søknadK9Format)

                } catch (e: Exception) {
                    return@RequestContext ServerResponse
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json()
                        .bodyValueAndAwait(e.localizedMessage)
                }
            }
        }
    }

    suspend fun validerSøknad(request: ServerRequest): ServerResponse {
        return RequestContext(coroutineContext, request) {
            val soknadTilValidering = request.body(BodyExtractors.toMono(OmsorgspengerKroniskSyktBarnSøknadDto::class.java))
            .awaitFirst()

            val søknadEntitet = mappeService.hentSøknad(soknadTilValidering.soeknadId)
                ?: return@RequestContext ServerResponse
                    .badRequest()
                    .buildAndAwait()

            val journalPoster = søknadEntitet.journalposter!!
            val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

            val mapTilEksternFormat: Pair<Søknad, List<Feil>>?

            try {
                mapTilEksternFormat = MapOmsKSBTilK9Format(
                    søknadId = soknadTilValidering.soeknadId,
                    journalpostIder = journalposterDto.journalposter,
                    dto = soknadTilValidering
                ).søknadOgFeil()
            } catch (e: Exception) {
                return@RequestContext ServerResponse
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

                return@RequestContext ServerResponse
                    .status(HttpStatus.BAD_REQUEST)
                    .json()
                    .bodyValueAndAwait(SøknadFeil(soknadTilValidering.soeknadId, feil))
            }
            return@RequestContext ServerResponse
                .status(HttpStatus.ACCEPTED)
                .json()
                .bodyValueAndAwait(søknad)
        }
    }
}