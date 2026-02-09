package no.nav.k9punsj.sak

import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class SakerRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val sakService: SakService,
    private val innloggetUtils: InnloggetUtils
) {

    internal companion object {
        private val logger: Logger = LoggerFactory.getLogger(SakerRoutes::class.java)
    }

    internal object Urls {
        internal const val HentSaker = "/saker/hent"
    }


    @Bean
    fun SakerRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HentSaker}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innloggetUtils.harInnloggetBrukerTilgangTilÅSendeInn(fnr = norskIdent, url = Urls.HentSaker)
                    ?.let { return@RequestContext it }

                val saker = try {
                    sakService.hentSaker(norskIdent)
                } catch (e: Exception) {
                    logger.error("Feilet med å hente saker.", e)
                    return@RequestContext ServerResponse
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json()
                        .bodyValueAndAwait(OasFeil(e.message))
                }

                return@RequestContext ServerResponse
                    .status(HttpStatus.OK)
                    .json()
                    .bodyValueAndAwait(saker)
            }
        }
    }
}
