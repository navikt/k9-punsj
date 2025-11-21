package no.nav.k9punsj.pleiepengersyktbarn

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import no.nav.k9punsj.utils.ServerRequestUtils.mapMatchFagsak
import no.nav.k9punsj.utils.ServerRequestUtils.mapNySøknad
import no.nav.k9punsj.utils.ServerRequestUtils.mapSendSøknad
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class PleiepengerSyktBarnRoutes(
    private val pleiepengerSyktBarnService: PleiepengerSyktBarnService,
    private val k9SakService: K9SakService,
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils
) {

    private companion object {
        private const val søknadType = "pleiepenger-sykt-barn-soknad"
        private const val SøknadIdKey = "soeknad_id"
    }

    internal object Urls {
        internal const val HenteMappe = "/$søknadType/mappe" // get
        internal const val HenteSøknad = "/$søknadType/mappe/{$SøknadIdKey}" // get
        internal const val NySøknad = "/$søknadType" // post
        internal const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" // put
        internal const val SendEksisterendeSøknad = "/$søknadType/send" // post
        internal const val ValiderSøknad = "/$søknadType/valider" // post
        internal const val HentInfoFraK9sak = "/$søknadType/k9sak/info" // post
        internal const val HentInfoFraK9sakMedSaksnummer = "/${søknadType}/k9sak/info/saksnummer" // post
    }

    @Bean
    fun pleiepengerSyktBarnSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilÅSendeInn(fnr = norskIdent, url = Urls.HenteMappe)
                    ?.let { return@RequestContext it }

                pleiepengerSyktBarnService.henteMappe(norskIdent)
            }
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknadId = request.pathVariable(SøknadIdKey)
                pleiepengerSyktBarnService.henteSøknad(søknadId)
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.mapPleiepengerSøknad()

                pleiepengerSyktBarnService.oppdaterEksisterendeSøknad(request, søknad)
            }
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.mapSendSøknad()
                innlogget.harInnloggetBrukerTilgangTilÅSendeInn(
                    fnr = søknad.norskIdent,
                    url = Urls.SendEksisterendeSøknad
                )?.let { return@RequestContext it }

                pleiepengerSyktBarnService.sendEksisterendeSøknad(søknad)
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.mapNySøknad()
                innlogget.harInnloggetBrukerTilgangTilÅSendeInn(
                    fnr = søknad.norskIdent,
                    url = Urls.NySøknad
                )?.let { return@RequestContext it }

                pleiepengerSyktBarnService.nySøknad(request, søknad)
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.mapPleiepengerSøknad()
                søknad.soekerId?.let { norskIdent ->
                    innlogget.harInnloggetBrukerTilgangTilÅSendeInn(
                        fnr = norskIdent,
                        url = Urls.ValiderSøknad
                    )?.let { return@RequestContext it }
                }

                pleiepengerSyktBarnService.validerSøknad(søknad)
            }
        }

        POST("/api${Urls.HentInfoFraK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val matchfagsak = request.mapMatchFagsak()
                innlogget.harInnloggetBrukerTilgangTilÅSendeInn(
                    fnr = listOf(matchfagsak.brukerIdent, matchfagsak.barnIdent!!),
                    fnrForSporingslogg = listOf(matchfagsak.brukerIdent, matchfagsak.barnIdent!!),
                    url = Urls.HentInfoFraK9sak
                )?.let { return@RequestContext it }

                pleiepengerSyktBarnService.hentInfoFraK9Sak(matchfagsak)
            }
        }

        POST("/api${Urls.HentInfoFraK9sakMedSaksnummer}") { request ->
            RequestContext(coroutineContext, request) {
                val saksnummer = request.queryParam("saksnummer").orElseThrow()
                val (perioder, _) = k9SakService.hentPerioderSomFinnesIK9ForSaksnummer(saksnummer)

                if (perioder != null) {
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

    private suspend fun ServerRequest.mapPleiepengerSøknad() =
        body(BodyExtractors.toMono(PleiepengerSyktBarnSøknadDto::class.java)).awaitFirst()
}
