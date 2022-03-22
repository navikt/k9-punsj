package no.nav.k9punsj.tilgangskontroll

import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.http.HttpRequest
import no.nav.security.token.support.core.jwt.JwtToken
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
            )
    )


    init {
        logger.info("Konfigurerte issuers = ${multiIssuerProperties.issuer}")

    }

    internal suspend fun authenticatedRequest(
            serverRequest: ServerRequest,
            issuerNames: Set<String>,
            isAccepted: (jwtToken: JwtToken) -> Boolean,
            requestedOperation: suspend (serverRequest: ServerRequest) -> ServerResponse
    ) : ServerResponse {
        val jwtToken = try {
            jwtTokenValidationHandler.getValidatedTokens(ServerHttpRequest(serverRequest)).issuers.intersect(issuerNames).firstOrNull()?.let {
                jwtTokenValidationHandler.getValidatedTokens(ServerHttpRequest(serverRequest)).getJwtToken(it)
            }
        } catch (cause: Throwable) {
            logger.warn("Feil ved validering av token", cause)
            null
        }
        return when {
            jwtToken == null -> {
                ServerResponse.status(HttpStatus.UNAUTHORIZED).buildAndAwait()
            }
            isAccepted(jwtToken) -> {
                requestedOperation(serverRequest)
            }
            else -> {
                ServerResponse.status(HttpStatus.FORBIDDEN).buildAndAwait()
            }
        }
    }
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
