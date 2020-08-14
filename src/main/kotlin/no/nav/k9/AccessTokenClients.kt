package no.nav.k9

import com.nimbusds.jose.jwk.JWK
import no.nav.helse.dusseldorf.oauth2.client.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
internal class AccessTokenClients (
        @Value("\${no.nav.security.jwt.client.azure.client_id}") azureClientId: String,
        @Value("\${no.nav.security.jwt.client.azure.jwk}") azureJwk: String,
        @Value("\${no.nav.security.jwt.client.azure.token_endpoint}") azureTokenEndpoint: URI
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(AccessTokenClients::class.java)
    }

    init {
        logger.info("AzureClientId=$azureClientId")
        logger.info("AzureTokenEndpoint=$azureTokenEndpoint")
    }

    private val keyId = try {
        val jwk = JWK.parse(azureJwk)
        requireNotNull(jwk.keyID) { "Azure JWK inneholder ikke keyID." }
        jwk.keyID
    } catch (_: Throwable) {
        throw IllegalArgumentException("Azure JWK på feil format.")
    }

    private val signedJwtAzureAccessTokenClient = SignedJwtAccessTokenClient(
            clientId = azureClientId,
            privateKeyProvider = FromJwk(azureJwk),
            keyIdProvider = DirectKeyId(keyId),
            tokenEndpoint = azureTokenEndpoint
    )

    @Bean
    @Qualifier("saf")
    internal fun safAccessTokenClient() : AccessTokenClient = signedJwtAzureAccessTokenClient
}


private val logger: Logger = LoggerFactory.getLogger("no.nav.k9.AccessTokenHelsesjekk")
internal fun AccessTokenClient.helsesjekk(
        scopes: Set<String>,
        operasjon: String,
        initialHealth: Health = Health.up().build()
) : Health {
    val builder = Health.Builder(initialHealth.status, initialHealth.details)
    return try {
        getAccessToken(scopes)
        builder.withDetail("$operasjon-access-token", "OK!").status(initialHealth.status)
    } catch (cause: Throwable) {
        logger.warn("Feil ved henting av access token for $operasjon. ${cause.message}")
        builder.withDetail("$operasjon-access-token", "Feilet!").down()
    }.build()
}