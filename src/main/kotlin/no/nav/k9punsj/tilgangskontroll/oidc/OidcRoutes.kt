package no.nav.k9punsj.tilgangskontroll.oidc

import kotlinx.coroutines.currentCoroutineContext
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Configuration
internal class OidcRoutes(
    private val authenticationHandler: AuthenticationHandler
) {

    internal object Urls {
        internal const val HentNavTokenHeader = "/oidc/hentNavTokenHeader"
    }

    @Bean
    fun OidcRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HentNavTokenHeader}") { request ->
            RequestContext(currentCoroutineContext(), request) {
                val clientHeader = request.headers().header("Authorization")
                ServerResponse
                    .ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValueAndAwait(clientHeader[0])
            }
        }
    }
}
