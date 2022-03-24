package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.domenetjenester.dto.JournalposterDto
import no.nav.k9punsj.domenetjenester.dto.SøknadFeil
import no.nav.k9punsj.domenetjenester.dto.SøknadIdDto
import no.nav.k9punsj.domenetjenester.dto.hentUtJournalposter
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleService
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.norskIdent
import no.nav.k9punsj.utils.ServerRequestUtils.opprettNy
import no.nav.k9punsj.utils.ServerRequestUtils.sendSøknad
import no.nav.k9punsj.utils.ServerRequestUtils.søknadLocation
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.coroutines.coroutineContext

@Configuration
internal class OmsorgspengerKroniskSyktBarnRoutes(
    private val objectMapper: ObjectMapper,
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
    private val mappeService: MappeService,
    private val personService: PersonService,
    private val punsjbolleService: PunsjbolleService,
    private val azureGraphService: IAzureGraphService,
    private val journalpostRepository: JournalpostRepository,
    private val soknadService : SoknadService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerKroniskSyktBarnRoutes::class.java)
        private const val søknadType = FagsakYtelseTypeUri.OMSORGSPENGER_KRONISK_SYKT_BARN
        private const val SøknadIdKey = "soeknad_id"
    }

    internal object Urls {
        internal const val HenteMappe = "/$søknadType/mappe" //get
        internal const val HenteSøknad = "/$søknadType/mappe/{$SøknadIdKey}" //get
        internal const val NySøknad = "/$søknadType" //post
        internal const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" //put
        internal const val SendEksisterendeSøknad = "/$søknadType/send" //post
        internal const val ValiderSøknad = "/$søknadType/valider" //post
    }

    @Bean
    fun omsorgspengerKroniskSyktBarnSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.norskIdent()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent,
                    Urls.HenteMappe
                )?.let { return@RequestContext it }

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

        GET("/api${Urls.HenteSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknadId = request.søknadId()
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

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val opprettNySøknad = request.opprettNy()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(opprettNySøknad.norskIdent,
                    Urls.NySøknad
                )?.let { return@RequestContext it }

                //oppretter sak i k9-sak hvis det ikke finnes fra før
                if (opprettNySøknad.pleietrengendeIdent != null) {
                    punsjbolleService.opprettEllerHentFagsaksnummer(
                        søker = opprettNySøknad.norskIdent,
                        pleietrengende = opprettNySøknad.pleietrengendeIdent,
                        journalpostId = opprettNySøknad.journalpostId,
                        periode = null,
                        correlationId = coroutineContext.hentCorrelationId(),
                        fagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS
                    )
                }

                //setter riktig type der man jobber på en ukjent i utgangspunktet
                journalpostRepository.settFagsakYtelseType(FagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN, opprettNySøknad.journalpostId)

                val søknadEntitet = mappeService.førsteInnsendingOmsKSB(
                    nySøknad = opprettNySøknad!!
                )
                return@RequestContext ServerResponse
                    .created(request.søknadLocation(søknadEntitet.søknadId))
                    .json()
                    .bodyValueAndAwait(søknadEntitet.tilOmsKSBvisning())
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.omsorgspengerKroniskSyktBarnSøknad()
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

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val sendSøknad = request.sendSøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(sendSøknad.norskIdent,
                    Urls.SendEksisterendeSøknad
                )?.let { return@RequestContext it }
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
                                .bodyValueAndAwait(OasFeil(feilen))
                        }

                        return@RequestContext ServerResponse
                            .accepted()
                            .json()
                            .bodyValueAndAwait(søknadK9Format)

                    } catch (e: Exception) {
                        val sw = StringWriter()
                        e.printStackTrace(PrintWriter(sw))
                        val exceptionAsString = sw.toString()
                        return@RequestContext ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .json()
                            .bodyValueAndAwait(OasFeil(exceptionAsString))
                    }
                }
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val soknadTilValidering = request.omsorgspengerKroniskSyktBarnSøknad()
                soknadTilValidering.soekerId?.let { norskIdent ->
                    innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                        norskIdent,
                        Urls.ValiderSøknad
                    )?.let { return@RequestContext it }
                }
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
                    val sw = StringWriter()
                    e.printStackTrace(PrintWriter(sw))
                    val exceptionAsString = sw.toString()
                    return@RequestContext ServerResponse
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json()
                        .bodyValueAndAwait(OasFeil(exceptionAsString))
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

    private fun ServerRequest.søknadId(): SøknadIdDto = pathVariable(SøknadIdKey)

    private suspend fun ServerRequest.omsorgspengerKroniskSyktBarnSøknad() =
        body(BodyExtractors.toMono(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)).awaitFirst()
}





