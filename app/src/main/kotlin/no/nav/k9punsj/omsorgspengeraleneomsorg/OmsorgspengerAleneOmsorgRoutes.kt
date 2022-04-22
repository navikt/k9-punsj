package no.nav.k9punsj.omsorgspengeraleneomsorg

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import no.nav.k9punsj.utils.ServerRequestUtils.mapNySøknad
import no.nav.k9punsj.utils.ServerRequestUtils.mapSendSøknad
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import kotlin.coroutines.coroutineContext

@Configuration
internal class OmsorgspengerAleneOmsorgRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
    private val omsorgspengerAleneOmsorgService: OmsorgspengerAleneOmsorgService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerAleneOmsorgRoutes::class.java)
        private const val søknadType = "omsorgspenger-alene-om-omsorgen-soknad"
        private const val søknadIdKey = "soeknad_id"
    }

    internal object Urls {
        internal const val HenteMappe = "/$søknadType/mappe" //get
        internal const val HenteSøknad = "/$søknadType/mappe/{$søknadIdKey}" //get
        internal const val NySøknad = "/$søknadType" //post
        internal const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" //put
        internal const val SendEksisterendeSøknad = "/$søknadType/send" //post
        internal const val ValiderSøknad = "/$søknadType/valider" //post
    }

    @Bean
    fun omsorgspengerAleneOmsorgSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = norskIdent,
                    url = Urls.HenteMappe
                )?.let { return@RequestContext it }

                omsorgspengerAleneOmsorgService.henteMappe(norskIdent)
            }
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknadId = request.søknadId()
                omsorgspengerAleneOmsorgService.henteSøknad(søknadId)
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val nySøknad = request.mapNySøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = nySøknad.norskIdent,
                    url = Urls.NySøknad
                )?.let { return@RequestContext it }

                omsorgspengerAleneOmsorgService.nySøknad(request, nySøknad)
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.omsorgspengerAleneOmsorgSøknad()
                omsorgspengerAleneOmsorgService.oppdaterEksisterendeSøknad(søknad)
            }
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val sendSøknad = request.mapSendSøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = sendSøknad.norskIdent,
                    url = Urls.SendEksisterendeSøknad
                )?.let { return@RequestContext it }

                omsorgspengerAleneOmsorgService.sendEksisterendeSøknad(sendSøknad)
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val soknadTilValidering = request.omsorgspengerAleneOmsorgSøknad()
                soknadTilValidering.soekerId?.let { norskIdent ->
                    innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                        norskIdent = norskIdent,
                        url = Urls.ValiderSøknad
                    )?.let { return@RequestContext it }
                }

                omsorgspengerAleneOmsorgService.validerSøknad(soknadTilValidering)
            }
        }
    }

    private fun ServerRequest.søknadId(): String = pathVariable(søknadIdKey)

    private suspend fun ServerRequest.omsorgspengerAleneOmsorgSøknad() =
        body(BodyExtractors.toMono(OmsorgspengerAleneOmsorgSøknadDto::class.java)).awaitFirst()
}
