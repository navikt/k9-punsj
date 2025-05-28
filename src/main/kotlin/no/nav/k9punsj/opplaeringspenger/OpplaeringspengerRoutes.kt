package no.nav.k9punsj.opplaeringspenger

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
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
internal class OpplaeringspengerRoutes(
    private val opplaeringspengerService: OpplaeringspengerService,
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils
) {

    private companion object {
        private const val søknadType = "opplaeringspenger-soknad"
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
        internal const val HentInstitusjoner = "/$søknadType/institusjoner" // get
    }

    @Bean
    fun opplaeringspengerSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = norskIdent, url = Urls.HenteMappe)
                    ?.let { return@RequestContext it }

                opplaeringspengerService.henteMappe(norskIdent)
            }
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknadId = request.pathVariable(SøknadIdKey)
                opplaeringspengerService.henteSøknad(søknadId)
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.mapOpplaeringspengerSøknad()

                opplaeringspengerService.oppdaterEksisterendeSøknad(request, søknad)
            }
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.mapSendSøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = søknad.norskIdent,
                    url = Urls.SendEksisterendeSøknad
                )?.let { return@RequestContext it }

                opplaeringspengerService.sendEksisterendeSøknad(søknad)
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.mapNySøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = søknad.norskIdent,
                    url = Urls.NySøknad
                )?.let { return@RequestContext it }

                opplaeringspengerService.nySøknad(request, søknad)
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.mapOpplaeringspengerSøknad()
                søknad.soekerId?.let { norskIdent ->
                    innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                        norskIdent = norskIdent,
                        url = Urls.ValiderSøknad
                    )?.let { return@RequestContext it }
                }

                opplaeringspengerService.validerSøknad(søknad)
            }
        }

        POST("/api${Urls.HentInfoFraK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val matchfagsak = request.mapMatchFagsak()
                innlogget.harInnloggetBrukerTilgangTil(
                    norskIdentDto = listOf(matchfagsak.brukerIdent, matchfagsak?.barnIdent).filterNotNull(),
                    url = Urls.HentInfoFraK9sak
                )?.let { return@RequestContext it }

                opplaeringspengerService.hentInfoFraK9Sak(matchfagsak)
            }
        }

        GET("/api${Urls.HentInstitusjoner}") { request ->
            RequestContext(coroutineContext, request) {
                val institusjoner = opplaeringspengerService.hentInstitusjoner()

                ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(institusjoner)
            }
        }
    }

    private suspend fun ServerRequest.mapOpplaeringspengerSøknad() =
        body(BodyExtractors.toMono(OpplaeringspengerSøknadDto::class.java)).awaitFirst()
}
