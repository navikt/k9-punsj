package no.nav.k9.oidc

import no.nav.k9.AuthenticationHandler
import no.nav.k9.RequestContext
import no.nav.k9.Routes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import kotlin.coroutines.coroutineContext


@Configuration
internal class OidcRoutes(
        private val authenticationHandler: AuthenticationHandler,
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(OidcRoutes::class.java)
    }

    internal object Urls {
        internal const val HentNavTokenHeader = "/oidc/hentNavTokenHeader/"
    }

    @Bean
    fun PdlRoutes() = Routes(authenticationHandler) {
        POST("/api${Urls.HentNavTokenHeader}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(request.headers())

            }

        }
    }
}