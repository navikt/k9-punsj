package no.nav.k9punsj.rest.web.ruter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.abac.IPepClient
import no.nav.k9punsj.azuregraph.IAzureGraphService
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.PleiepengerSyktBarnSoknadService
import no.nav.k9punsj.domenetjenester.mappers.MapFraVisningTilEksternFormat
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9Format
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakService
import no.nav.k9punsj.rest.web.HentSøknad
import no.nav.k9punsj.rest.web.Matchfagsak
import no.nav.k9punsj.rest.web.OpprettNySøknad
import no.nav.k9punsj.rest.web.SendSøknad
import no.nav.k9punsj.rest.web.dto.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import kotlin.coroutines.coroutineContext

@Configuration
internal class PleiepengerSyktBarnRoutes(
    private val objectMapper: ObjectMapper,
    private val mappeService: MappeService,
    private val pleiepengerSyktBarnSoknadService: PleiepengerSyktBarnSoknadService,
    private val personService: PersonService,
    private val authenticationHandler: AuthenticationHandler,
    private val pepClient: IPepClient,
    private val azureGraphService: IAzureGraphService,
    private val k9SakService: K9SakService,
    private val journalpostRepository: JournalpostRepository,

    ) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnRoutes::class.java)

        private const val søknadType = FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN
        private const val SøknadIdKey = "soeknad_id"
    }

    internal object Urls {
        internal const val HenteMappe = "/$søknadType/mappe" //get
        internal const val HenteSøknad = "/$søknadType/mappe/{$SøknadIdKey}" //get
        internal const val NySøknad = "/$søknadType" //post
        internal const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" //put
        internal const val SendEksisterendeSøknad = "/$søknadType/send" //post
        internal const val ValiderSøknad = "/$søknadType/valider" //post
        internal const val HentSøknadFraK9Sak = "/k9-sak/$søknadType" //post
        internal const val HentInfoFraK9sak = "/$søknadType/k9sak/info" //post
    }

    @Bean
    fun pleiepengerSyktBarnSøknadRoutes() = Routes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.norskeIdent()
                harInnloggetBrukerTilgangTilSøker(norskIdent)?.let { return@RequestContext it }

                val person = personService.finnPersonVedNorskIdent(norskIdent)
                if (person != null) {
                    val svarDto = mappeService.hentMappe(
                        person = person,
                        søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                    ).tilPsbVisning(norskIdent)
                    return@RequestContext ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(svarDto)
                }
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(SvarDto(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf()))
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
                        .bodyValueAndAwait(søknad.tilPsbvisning())
                }
                return@RequestContext ServerResponse
                    .notFound()
                    .buildAndAwait()
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.pleiepengerSøknad()
                val saksbehandler = azureGraphService.hentIdentTilInnloggetBruker()


                val søknadEntitet = mappeService.utfyllendeInnsending(
                    søknad = søknad,
                    saksbehandler = saksbehandler
                )

                if (søknadEntitet == null) {
                    ServerResponse
                        .badRequest()
                        .buildAndAwait()
                } else {
                    val søker = personService.finnPerson(søknadEntitet.first.søkerId)
                    journalpostRepository.settKildeHvisIkkeFinnesFraFør(hentUtJournalposter(søknadEntitet.first),
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
                harInnloggetBrukerTilgangTilSøker(sendSøknad.norskIdent)?.let { return@RequestContext it }
                val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)

                if (søknadEntitet == null) {
                    return@RequestContext ServerResponse
                        .badRequest()
                        .buildAndAwait()
                } else {
                    try {
                        val søknad: PleiepengerSøknadVisningDto = objectMapper.convertValue(søknadEntitet.søknad!!)
                        val format = MapFraVisningTilEksternFormat.mapTilSendingsformat(søknad)

                        val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(format)?.first ?: emptyList()

                        val journalPoster = søknadEntitet.journalposter!!
                        val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

                        val søknadK9Format = MapTilK9Format.mapTilEksternFormat(format,
                            søknad.soeknadId,
                            hentPerioderSomFinnesIK9,
                            journalposterDto.journalposter)
                        if (søknadK9Format.second.isNotEmpty()) {
                            val feil = søknadK9Format.second.map { feil ->
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

                        pleiepengerSyktBarnSoknadService.sendSøknad(
                            søknadK9Format.first,
                            journalposterDto.journalposter
                        )

                        return@RequestContext ServerResponse
                            .accepted()
                            .buildAndAwait()
                    } catch (e: Exception) {
                        logger.error("", e)
                        return@RequestContext ServerResponse
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .buildAndAwait()
                    }
                }
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val opprettNySøknad = request.opprettNy()
                harInnloggetBrukerTilgangTilSøker(opprettNySøknad.norskIdent)?.let { return@RequestContext it }
                val søknadEntitet = mappeService.førsteInnsending(
                    nySøknad = opprettNySøknad!!,
                    søknadType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
                )

                return@RequestContext ServerResponse
                    .created(request.søknadLocation(søknadEntitet.søknadId))
                    .json()
                    .bodyValueAndAwait(søknadEntitet.tilPsbvisning())
            }
        }

        POST("/api${Urls.HentSøknadFraK9Sak}") { request ->
            RequestContext(coroutineContext, request) {
                val hentSøknad = request.hentSøknad()
                harInnloggetBrukerTilgangTilSøker(hentSøknad.norskIdent)?.let { return@RequestContext it }

                //TODO(OJR) koble på mot endepunkt i k9-sak
                val søknadDto = PleiepengerSøknadVisningDto(
                    soeknadId = "123",
                    soekerId = hentSøknad.norskIdent,
                    journalposter = null,
                )

                val svarDto =
                    SvarDto(hentSøknad.norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf(søknadDto))

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(svarDto)
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val sendSøknad = request.sendSøknad()
                harInnloggetBrukerTilgangTilSøker(sendSøknad.norskIdent)?.let { return@RequestContext it }
                val søknadEntitet = mappeService.hentSøknad(sendSøknad.soeknadId)
                    ?: return@RequestContext ServerResponse
                        .badRequest()
                        .buildAndAwait()

                val søknad: PleiepengerSøknadVisningDto = objectMapper.convertValue(søknadEntitet.søknad!!)
                val format = MapFraVisningTilEksternFormat.mapTilSendingsformat(søknad)

                val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(format)?.first ?: emptyList()
                val journalPoster = søknadEntitet.journalposter!!
                val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

                val søknadK9Format = MapTilK9Format.mapTilEksternFormat(format,
                    søknad.soeknadId,
                    hentPerioderSomFinnesIK9,
                    journalposterDto.journalposter)

                if (søknadK9Format.second.isNotEmpty()) {
                    val feil = søknadK9Format.second.map { feil ->
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

                return@RequestContext ServerResponse
                    .status(HttpStatus.ACCEPTED)
                    .buildAndAwait()
            }
        }


        POST("/api${Urls.HentInfoFraK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val matchfagsak = request.matchFagsak()
                harInnloggetBrukerTilgangTil(listOf(matchfagsak.brukerIdent,
                    matchfagsak.barnIdent))?.let { return@RequestContext it }

                val hentPerioderSomFinnesIK9 = k9SakService.hentPerioderSomFinnesIK9(
                    matchfagsak.brukerIdent,
                    matchfagsak.barnIdent,
                    FagsakYtelseType.PLEIEPENGER_SYKT_BARN)

                return@RequestContext if (hentPerioderSomFinnesIK9.first != null) {
                    val body = hentPerioderSomFinnesIK9.first!!
                    ServerResponse
                        .ok()
                        .json()
                        .bodyValueAndAwait(body)

                } else {
                    ServerResponse
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json()
                        .buildAndAwait()
                }
            }
        }
    }

    private suspend fun henterPerioderSomFinnesIK9sak(format: PleiepengerSøknadMottakDto): Pair<List<PeriodeDto>?, String?>? {
        if (format.søker?.norskIdentitetsnummer == null || format.ytelse?.barn?.norskIdentitetsnummer == null) {
            return null
        }
        return k9SakService.hentPerioderSomFinnesIK9(format.søker.norskIdentitetsnummer,
            format.ytelse.barn.norskIdentitetsnummer,
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
    }

    private suspend fun harInnloggetBrukerTilgangTil(norskIdentDto: List<NorskIdentDto>): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.harBasisTilgang(norskIdentDto)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til å slå opp denne personen")
        }
        return null
    }

    private suspend fun harInnloggetBrukerTilgangTilSøker(norskIdentDto: NorskIdentDto): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.harBasisTilgang(norskIdentDto)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til å slå opp denne personen")
        }
        return null
    }

    private suspend fun ServerRequest.norskeIdent(): String {
        return headers().header("X-Nav-NorskIdent").first()!!
    }

    private suspend fun ServerRequest.pleiepengerSøknad() =
        body(BodyExtractors.toMono(PleiepengerSøknadVisningDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.opprettNy() =
        body(BodyExtractors.toMono(OpprettNySøknad::class.java)).awaitFirst()

    private suspend fun ServerRequest.hentSøknad() = body(BodyExtractors.toMono(HentSøknad::class.java)).awaitFirst()
    private suspend fun ServerRequest.sendSøknad() = body(BodyExtractors.toMono(SendSøknad::class.java)).awaitFirst()
    private suspend fun ServerRequest.matchFagsak() = body(BodyExtractors.toMono(Matchfagsak::class.java)).awaitFirst()

    private fun ServerRequest.søknadLocation(søknadId: SøknadIdDto) =
        uriBuilder().pathSegment("mappe", søknadId).build()

    private suspend fun ServerRequest.søknadId(): SøknadIdDto = pathVariable(SøknadIdKey)
}
