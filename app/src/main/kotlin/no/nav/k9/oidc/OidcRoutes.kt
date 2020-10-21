package no.nav.k9.oidc

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.AuthenticationHandler
import no.nav.k9.RequestContext
import no.nav.k9.Routes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import kotlin.coroutines.coroutineContext


@Configuration
internal class OidcRoutes(
        private val authenticationHandler: AuthenticationHandler,
        @Qualifier("sts") accessTokenClient: AccessTokenClient
) {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val scope: Set<String> = setOf("openid")
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(OidcRoutes::class.java)
    }

    internal object Urls {
        internal const val HentNavTokenHeader = "/oidc/hentNavTokenHeader"
    }

    @Bean
    fun OidcRoutes() = Routes(authenticationHandler) {
        GET("/api${Urls.HentNavTokenHeader}") { request ->
            val navHeader = cachedAccessTokenClient.getAccessToken(scope)
                    .asAuthoriationHeader()

            RequestContext(coroutineContext, request) {
                val clientHeader = request.headers().header("Authorization")
                ServerResponse
                        .ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .bodyValueAndAwait(clientHeader + "\n" + navHeader)
            }
        }
    }
}