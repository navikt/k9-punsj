package no.nav.k9

import com.nimbusds.jose.jwk.JWK
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.DirectKeyId
import no.nav.helse.dusseldorf.oauth2.client.FromJwk
import no.nav.helse.dusseldorf.oauth2.client.SignedJwtAccessTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.net.URI

@Service
internal class AuthenticationClient (
        @Value("\${no.nav.security.jwt.client.azure.client_id}") azureClientId: String,
        @Value("\${no.nav.security.jwt.client.azure.jwk}") azureJwk: String,
        @Value("\${no.nav.security.jwt.client.azure.token_endpoint}") azureTokenEndpoint: URI
) : ReactiveHealthIndicator {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(AuthenticationClient::class.java)
    }

    private val keyId = try {
        val jwk = JWK.parse(azureJwk)
        requireNotNull(jwk.keyID) { "Azure JWK inneholder ikke keyID." }
        jwk.keyID
    } catch (_: Throwable) {
        throw IllegalArgumentException("Azure JWK på feil format.")
    }

    init {
        logger.info("AzureClientId=$azureClientId")
        logger.info("AzureTokenEndpoint=$azureTokenEndpoint")
    }


    private val signedJwtAccessTokenClient = SignedJwtAccessTokenClient(
            clientId = azureClientId,
            privateKeyProvider = FromJwk(azureJwk),
            keyIdProvider = DirectKeyId(keyId),
            tokenEndpoint = azureTokenEndpoint
    )

    internal val accessTokenClient = CachedAccessTokenClient(signedJwtAccessTokenClient)

    override fun health() = Mono.just(try {
        signedJwtAccessTokenClient.getAccessToken(setOf(
                "4bd971d8-2469-434f-9322-8cfe7a7a3379/.default"
        ))
        Health.up().build()
    } catch (cause: Throwable) {
        logger.info("Feil ved henting av access token ${cause.message}")
        Health.down().withException(cause).build()
    })
}
