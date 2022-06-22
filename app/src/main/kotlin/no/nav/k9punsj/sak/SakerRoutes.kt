package no.nav.k9punsj.sak

import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    private val innloggetUtils: InnloggetUtils,
    @Value("\${SAKER_ENABLED}") private val sakerEnabled: Boolean
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
            if (sakerEnabled) {
                RequestContext(coroutineContext, request) {
                    val norskIdent = request.hentNorskIdentHeader()
                    innloggetUtils.harInnloggetBrukerTilgangTil(norskIdentDto = listOf(norskIdent), url = Urls.HentSaker)
                        ?.let { return@RequestContext it }

                    return@RequestContext sakService.hentSaker(norskIdent)
                }
            } else {
                return@GET ServerResponse
                    .status(HttpStatus.NOT_IMPLEMENTED)
                    .json()
                    .bodyValueAndAwait("Ikke enablet")
            }
        }
    }
}
