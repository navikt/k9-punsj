package no.nav.k9punsj.rest.web.ruter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.abac.IPepClient
import no.nav.k9punsj.azuregraph.IAzureGraphService
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.domenetjenester.MappeService
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.PleiepengerSyktBarnSoknadService
import no.nav.k9punsj.domenetjenester.mappers.MapFraVisningTilEksternFormat
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9Format
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.rest.eksternt.k9sak.K9SakService
import no.nav.k9punsj.rest.eksternt.punsjbollen.PunsjbolleService
import no.nav.k9punsj.rest.web.Matchfagsak
import no.nav.k9punsj.rest.web.OpprettNySøknad
import no.nav.k9punsj.rest.web.SendSøknad
import no.nav.k9punsj.rest.web.dto.*
import no.nav.k9punsj.rest.web.openapi.OasFeil
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
    private val punsjbolleService: PunsjbolleService,

    ) {
    private companion object {
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
        internal const val HentInfoFraK9sak = "/$søknadType/k9sak/info" //post
    }

    @Bean
    fun pleiepengerSyktBarnSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.norskeIdent()
                harInnloggetBrukerTilgangTilOgSendeInn(norskIdent, Urls.HenteMappe)?.let { return@RequestContext it }

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
                harInnloggetBrukerTilgangTilOgSendeInn(sendSøknad.norskIdent,
                    Urls.SendEksisterendeSøknad)?.let { return@RequestContext it }
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

                        if (!søknadK9Format.first.journalposter.map { j -> j.journalpostId }
                                .containsAll(journalposterDto.journalposter)) {
                            throw IllegalStateException("missmatch mellom journalposter -> Dette skal ikke skje")
                        }

                        val feil = pleiepengerSyktBarnSoknadService.sendSøknad(
                            søknadK9Format.first,
                            journalposterDto.journalposter
                        )

                        if (feil != null) {
                            return@RequestContext ServerResponse
                                .status(feil.first)
                                .json()
                                .bodyValueAndAwait(OasFeil(feil.second))
                        }
                        return@RequestContext ServerResponse
                            .accepted()
                            .json()
                            .bodyValueAndAwait(søknadK9Format.first)

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
                harInnloggetBrukerTilgangTilOgSendeInn(opprettNySøknad.norskIdent,
                    Urls.NySøknad)?.let { return@RequestContext it }

                //oppretter sak i k9-sak hvis det ikke finnes fra før
                if (opprettNySøknad.barnIdent != null) {
                    punsjbolleService.opprettEllerHentFagsaksnummer(opprettNySøknad.norskIdent,
                        opprettNySøknad.barnIdent,
                        opprettNySøknad.journalpostId,
                        null,
                        coroutineContext.hentCorrelationId())
                }

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

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val soknadTilValidering = request.pleiepengerSøknad()
                soknadTilValidering.soekerId?.let {
                    harInnloggetBrukerTilgangTilOgSendeInn(
                        it,
                        Urls.ValiderSøknad)?.let { return@RequestContext it }
                }
                val søknadEntitet = mappeService.hentSøknad(soknadTilValidering.soeknadId)
                    ?: return@RequestContext ServerResponse
                        .badRequest()
                        .buildAndAwait()

                val format = MapFraVisningTilEksternFormat.mapTilSendingsformat(soknadTilValidering)

                val hentPerioderSomFinnesIK9 = henterPerioderSomFinnesIK9sak(format)?.first ?: emptyList()
                val journalPoster = søknadEntitet.journalposter!!
                val journalposterDto: JournalposterDto = objectMapper.convertValue(journalPoster)

                val mapTilEksternFormat: Pair<Søknad, List<Feil>>?

                try {
                    mapTilEksternFormat = MapTilK9Format.mapTilEksternFormat(format,
                        soknadTilValidering.soeknadId,
                        hentPerioderSomFinnesIK9,
                        journalposterDto.journalposter)
                } catch (e: Exception) {
                    val sw = StringWriter()
                    e.printStackTrace(PrintWriter(sw))
                    val exceptionAsString = sw.toString()
                    return@RequestContext ServerResponse
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json()
                        .bodyValueAndAwait(OasFeil(exceptionAsString))
                }

                if (mapTilEksternFormat.second.isNotEmpty()) {
                    val feil = mapTilEksternFormat.second.map { feil ->
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
                mappeService.utfyllendeInnsending(
                    søknad = soknadTilValidering,
                    saksbehandler = saksbehandler
                )
                return@RequestContext ServerResponse
                    .status(HttpStatus.ACCEPTED)
                    .json()
                    .bodyValueAndAwait(mapTilEksternFormat.first)
            }
        }

        POST("/api${Urls.HentInfoFraK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val matchfagsak = request.matchFagsak()
                harInnloggetBrukerTilgangTil(listOf(matchfagsak.brukerIdent,
                    matchfagsak.barnIdent), Urls.HentInfoFraK9sak)?.let { return@RequestContext it }

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
                        .ok()
                        .json()
                        .bodyValueAndAwait(listOf<PeriodeDto>())
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

    private suspend fun harInnloggetBrukerTilgangTil(norskIdentDto: List<NorskIdentDto>, url: String): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.sendeInnTilgang(norskIdentDto, url)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til å slå opp denne personen")
        }
        return null
    }

    private suspend fun harInnloggetBrukerTilgangTilOgSendeInn(
        norskIdentDto: NorskIdentDto,
        url: String,
    ): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.sendeInnTilgang(norskIdentDto, url)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til og sende på denne personen")
        }
        return null
    }

//    private suspend fun harInnloggetBrukerTilgangTilSøker(norskIdentDto: NorskIdentDto, url : String): ServerResponse? {
//        val saksbehandlerHarTilgang = pepClient.harBasisTilgang(norskIdentDto, url)
//        if (!saksbehandlerHarTilgang) {
//            return ServerResponse
//                .status(HttpStatus.FORBIDDEN)
//                .json()
//                .bodyValueAndAwait("Du har ikke lov til å slå opp denne personen")
//        }
//        return null
//    }

    private fun ServerRequest.norskeIdent(): String {
        return headers().header("X-Nav-NorskIdent").first()!!
    }

    private suspend fun ServerRequest.pleiepengerSøknad() =
        body(BodyExtractors.toMono(PleiepengerSøknadVisningDto::class.java)).awaitFirst()

    private suspend fun ServerRequest.opprettNy() =
        body(BodyExtractors.toMono(OpprettNySøknad::class.java)).awaitFirst()

    private suspend fun ServerRequest.sendSøknad() = body(BodyExtractors.toMono(SendSøknad::class.java)).awaitFirst()
    private suspend fun ServerRequest.matchFagsak() = body(BodyExtractors.toMono(Matchfagsak::class.java)).awaitFirst()

    private fun ServerRequest.søknadLocation(søknadId: SøknadIdDto) =
        uriBuilder().pathSegment("mappe", søknadId).build()

    private fun ServerRequest.søknadId(): SøknadIdDto = pathVariable(SøknadIdKey)
}
