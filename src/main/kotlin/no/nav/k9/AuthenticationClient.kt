package no.nav.k9

import com.nimbusds.jose.jwk.JWK
import net.minidev.json.JSONObject
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

    private val handeledJwk = try {
        JWK.parse(azureJwk)
        azureJwk
    } catch (cause: Throwable) {
        val jwk = mutableMapOf<String, String>()
        azureJwk
                .removePrefix("map[")
                .removeSuffix("]")
                .split(" ")
                .map { it.split(":") }
                .forEach {
                    jwk[it.first()] = it[1]
                }
        JSONObject(jwk).toJSONString()
    }

    init {
        logger.info("AzureClientId=$azureClientId")
        logger.info("AzureTokenEndpoint=$azureTokenEndpoint")
    }


    private val accessTokenClient = SignedJwtAccessTokenClient(
            clientId = azureClientId,
            privateKeyProvider = FromJwk(handeledJwk),
            keyIdProvider = DirectKeyId(JWK.parse(handeledJwk).keyID),
            tokenEndpoint = azureTokenEndpoint
    )

    override fun health() = Mono.just(try {
        accessTokenClient.getAccessToken(setOf(
                "4bd971d8-2469-434f-9322-8cfe7a7a3379/.default"
        ))
        Health.up().build()
    } catch (cause: Throwable) {
        logger.info("Feil ved henting av access token ${cause.message}")
        Health.down().withException(cause).build()
    })
}
