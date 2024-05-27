package no.nav.k9punsj.tilgangskontroll

import jakarta.validation.Valid
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

@Service
class AuthenticationHandler(
    multiIssuerProperties: MultiIssuerProperties
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(AuthenticationHandler::class.java)
    }

    private val jwtTokenValidationHandler = JwtTokenValidationHandler(
        MultiIssuerConfiguration(
            multiIssuerProperties.issuer
        )
    )

    init {
        logger.info("Konfigurerte issuers = ${multiIssuerProperties.issuer}")
    }

    /**
     * Utility function for handling authenticated requests in a Spring WebFlux application.
     * @property ServerRequest http request
     * @property issuerNames a set of issuerNames.
     * @property isAccepted a lambda function that determines whether a given JwtToken is acceptable,
     * @property requestedOperation and another lambda function that represents
     * the operation to be performed on the request if the token is acceptable.
     */
    internal suspend fun authenticatedRequest(
        serverRequest: ServerRequest,
        issuerNames: Set<String>,
        isAccepted: (jwtToken: JwtToken) -> Boolean,
        requestedOperation: suspend (serverRequest: ServerRequest) -> ServerResponse
    ): ServerResponse {
        val jwtToken = try {
            val request = ServerHttpRequest(serverRequest)
            jwtTokenValidationHandler.getValidatedTokens(request)
                .issuers
                .intersect(issuerNames)
                .firstOrNull()
                ?.let {
                    jwtTokenValidationHandler.getValidatedTokens(request).getJwtToken(it)
                }
        } catch (cause: Throwable) {
            logger.warn("Feil ved validering av token", cause)
            null
        } ?: return ServerResponse.status(HttpStatus.UNAUTHORIZED).buildAndAwait()

        return when (isAccepted(jwtToken)) {
            true -> requestedOperation(serverRequest)
            false -> ServerResponse.status(HttpStatus.FORBIDDEN).buildAndAwait()
        }
    }
}

private class ServerHttpRequest(private val serverRequest: ServerRequest) : HttpRequest {
    override fun getHeader(headerName: String) = serverRequest.headers().header(headerName).firstOrNull()
}

@Configuration
@ConfigurationProperties(prefix = "no.nav.security.jwt")
@EnableConfigurationProperties
@Validated
class MultiIssuerProperties {
    @Valid
    val issuer: Map<String, IssuerProperties> = HashMap()
}
