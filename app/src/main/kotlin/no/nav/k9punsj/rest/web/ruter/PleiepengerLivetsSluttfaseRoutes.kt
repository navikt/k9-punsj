package no.nav.k9punsj.rest.web.ruter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.azuregraph.IAzureGraphService
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.domenetjenester.mappers.MapPlsfTilK9Format
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakService
import no.nav.k9punsj.rest.eksternt.punsjbollen.PunsjbolleService
import no.nav.k9punsj.rest.web.*
import no.nav.k9punsj.rest.web.dto.*
import no.nav.k9punsj.rest.web.openapi.OasFeil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.*
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import kotlin.coroutines.coroutineContext

@Configuration
internal class PleiepengerLivetsSluttfaseRoutes(
    private val objectMapper: ObjectMapper,
    private val mappeService: MappeService,
    private val soknadService: SoknadService,
    private val personService: PersonService,
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
    private val azureGraphService: IAzureGraphService,
    private val k9SakService: K9SakService,
    private val journalpostRepository: JournalpostRepository,
    private val punsjbolleService: PunsjbolleService,
    @Value("\${no.nav.k9sak.frontend}") private val k9SakFrontend: URI,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PleiepengerLivetsSluttfaseRoutes::class.java)
        private val fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE
        private const val søknadType = FagsakYtelseTypeUri.PLEIEPENGER_LIVETS_SLUTTFASE
        private const val SøknadIdKey = "soeknad_id"
    }

    internal object Urls {
        internal const val HenteMappe = "/$søknadType/mappe" //get
        internal const val HenteSøknad = "/$søknadType/mappe/{$SøknadIdKey}" //get
        internal const val NySøknad = "/$søknadType" //post
        internal const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" //put
        internal const val SendEksisterendeSøknad = "/$søknadType/send" //post
        internal const val ValiderSøknad = "/$søknadType/valider" //post
        internal const val HentInfoFraK9sak = "/$søknadType/k9sak/info" //post
    }

    @Bean
    fun pleiepengerLivetsSluttfaseSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.norskIdent()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent, Urls.HenteMappe)
                    ?.let { return@RequestContext it }

                val person = personService.finnPersonVedNorskIdent(norskIdent)
                if (person != null) {
                    val svarDto = mappeService.hentMappe(
                        person = person
                    ).tilPlsVisning(norskIdent)
                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(svarDto)
                }
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(SvarPlsDto(norskIdent, fagsakYtelseType.kode, listOf()))
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
                        .bodyValueAndAwait(søknad.tilPlsvisning())
                }
                return@RequestContext ServerResponse
                    .notFound()
                    .buildAndAwait()
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.pleiepengerPlsSøknad()
                val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()

                val søknadEntitet = mappeService.utfyllendeInnsendingPls(
                    dto = søknad,
                    saksbehandler = saksbehandler
                )

                if (søknadEntitet == null) {
                    ServerResponse
                        .badRequest()
                        .buildAndAwait()
                } else {
                    val (entitet, _) = søknadEntitet
                    val søker = personService.finnPerson(entitet.søkerId)
                    journalpostRepository.settKildeHvisIkkeFinnesFraFør(hentUtJournalposter(entitet),
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
                    Urls.SendEksisterendeSøknad)?.let { return@RequestContext it }
                val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)

                if (søknadEntitet == null) {
                    return@RequestContext ServerResponse
                        .badRequest()
                        .buildAndAwait()
                } else {
                    try {
                        val søknad: PleiepengerLivetsSluttfaseSøknadDto =
                            objectMapper.convertValue(søknadEntitet.søknad!!)
                        val hentPerioderSomFinnesIK9 =
                            henterPerioderSomFinnesIK9sak(søknad.soekerId, søknad.pleietrengende)?.first ?: emptyList()

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
                            return@RequestContext ServerResponse
                                .status(HttpStatus.CONFLICT)
                                .bodyValueAndAwait(OasFeil("Innsendingen må inneholde minst en journalpost som kan sendes inn."))
                        }

                        val (søknadK9Format, feilISøknaden) = MapPlsfTilK9Format(
                            søknadId = søknad.soeknadId,
                            journalpostIder = journalpostIder,
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
                            .location(k9SakFrontendUrl(søknadK9Format))
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

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val opprettNySøknad = request.opprettNy()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(opprettNySøknad.norskIdent,
                    Urls.NySøknad)?.let { return@RequestContext it }

                //oppretter sak i k9-sak hvis det ikke finnes fra før
                if (opprettNySøknad.pleietrengendeIdent != null) {
                    punsjbolleService.opprettEllerHentFagsaksnummer(
                        søker = opprettNySøknad.norskIdent,
                        pleietrengende = opprettNySøknad.pleietrengendeIdent,
                        journalpostId = opprettNySøknad.journalpostId,
                        periode = null,
                        correlationId = coroutineContext.hentCorrelationId(),
                        fagsakYtelseType = no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
                }

                //setter riktig type der man jobber på en ukjent i utgangspunktet
                journalpostRepository.settFagsakYtelseType(fagsakYtelseType, opprettNySøknad.journalpostId)

                val søknadEntitet = mappeService.førsteInnsendingPsb(
                    nySøknad = opprettNySøknad!!
                )
                return@RequestContext ServerResponse
                    .created(request.søknadLocation(søknadEntitet.søknadId))
                    .json()
                    .bodyValueAndAwait(søknadEntitet.tilPlsvisning())
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val soknadTilValidering = request.pleiepengerPlsSøknad()
                soknadTilValidering.soekerId?.let { norskIdent ->
                    innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                        norskIdent,
                        Urls.ValiderSøknad)?.let { return@RequestContext it }
                }
                val søknadEntitet = mappeService.hentSøknad(soknadTilValidering.soeknadId)
                    ?: return@RequestContext ServerResponse
                        .badRequest()
                        .buildAndAwait()

                val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(soknadTilValidering.soekerId,
                    soknadTilValidering.pleietrengende)?.first ?: emptyList()
                val journalPoster = søknadEntitet.journalposter!!
                val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

                val mapTilEksternFormat: Pair<Søknad, List<Feil>>?

                try {
                    mapTilEksternFormat = MapPlsfTilK9Format(
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
                val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
                mappeService.utfyllendeInnsendingPls(
                    dto = soknadTilValidering,
                    saksbehandler = saksbehandler
                )
                return@RequestContext ServerResponse
                    .status(HttpStatus.ACCEPTED)
                    .json()
                    .bodyValueAndAwait(søknad)
            }
        }

        POST("/api${Urls.HentInfoFraK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val matchfagsak = request.matchFagsak()
                innlogget.harInnloggetBrukerTilgangTil(listOf(matchfagsak.brukerIdent,
                    matchfagsak.barnIdent), Urls.HentInfoFraK9sak)?.let { return@RequestContext it }

                val (perioder, _) = k9SakService.hentPerioderSomFinnesIK9(
                    matchfagsak.brukerIdent,
                    matchfagsak.barnIdent,
                    fagsakYtelseType)

                return@RequestContext if (perioder != null) {
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
        }
    }

    private suspend fun k9SakFrontendUrl(søknad: Søknad) = punsjbolleService.opprettEllerHentFagsaksnummer(
        søker = søknad.søker.personIdent.verdi,
        pleietrengende = søknad.getYtelse<PleipengerLivetsSluttfase>().pleietrengende.personIdent.verdi,
        søknad = søknad,
        correlationId = coroutineContext.hentCorrelationId()
    ).let { saksnummer -> URI("$k9SakFrontend/fagsak/${saksnummer.saksnummer}/behandling/") }

    private suspend fun henterPerioderSomFinnesIK9sak(
        søkerIdent: NorskIdentDto?,
        pleietrengendeDto: PleiepengerLivetsSluttfaseSøknadDto.PleietrengendeDto?,
    ): Pair<List<PeriodeDto>?, String?>? {
        if (søkerIdent.isNullOrBlank() || pleietrengendeDto == null || pleietrengendeDto.norskIdent.isNullOrBlank()) {
            return null
        }
        return k9SakService.hentPerioderSomFinnesIK9(
            søker = søkerIdent,
            barn = pleietrengendeDto.norskIdent,
            fagsakYtelseType = fagsakYtelseType
        )
    }

    private fun ServerRequest.søknadId(): SøknadIdDto = pathVariable(SøknadIdKey)
}
