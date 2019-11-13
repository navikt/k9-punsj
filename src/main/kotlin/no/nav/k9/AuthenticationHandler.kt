package no.nav.k9

import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever
import no.nav.security.token.support.core.http.HttpRequest
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URL
import java.util.HashMap
import javax.validation.Valid

@Service
internal class AuthenticationHandler(
        multiIssuerProperties: MultiIssuerProperties
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(AuthenticationHandler::class.java)
    }

    private val jwtTokenValidationHandler = JwtTokenValidationHandler(
            MultiIssuerConfiguration(
                    multiIssuerProperties.issuer,
                    resourceReceiver()
            )
    )

    private fun resourceReceiver() = System.getenv("HTTP_PROXY")?.let {
        ProxyAwareResourceRetriever(URL(it))
    }

    init {
        logger.info("Konfigurerte issuers = ${multiIssuerProperties.issuer}")

    }

    internal suspend fun authenticatedRequest(
            serverRequest: ServerRequest,
            requestedOperation: suspend (serverRequest: ServerRequest) -> ServerResponse
    ) : ServerResponse {
        val isValidToken = try {
            jwtTokenValidationHandler.getValidatedTokens(ServerHttpRequest(serverRequest)).hasValidToken()
        } catch (cause: Throwable) {
            logger.warn("Feil ved validering av token", cause)
            false
        }
        return if (isValidToken) {
            requestedOperation(serverRequest)
        } else ServerResponse.status(HttpStatus.UNAUTHORIZED).buildAndAwait()
    }
}

private data class NameValueCookie(private val name: String, private val value: String) : HttpRequest.NameValue {
    override fun getName() = name
    override fun getValue() = value
}

private class ServerHttpRequest(private val serverRequest: ServerRequest) : HttpRequest {
    override fun getCookies() = emptyArray<HttpRequest.NameValue>()
    override fun getHeader(headerNavn: String) = serverRequest.headers().header(headerNavn).firstOrNull()

}

@Configuration
@ConfigurationProperties(prefix = "no.nav.security.jwt")
@EnableConfigurationProperties
@Validated
class MultiIssuerProperties {
    @Valid
    val issuer: Map<String, IssuerProperties> = HashMap()
}
