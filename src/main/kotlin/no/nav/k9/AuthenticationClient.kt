package no.nav.k9

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
internal class AuthenticationClient(
        @Value("\${no.nav.security.jwt.client.azure.client_id}") azureClientId: String?,
        @Value("\${no.nav.security.jwt.client.azure.client_secret}") azureClientSecret: String?,
        @Value("\${no.nav.security.jwt.client.azure.jwk}") azureJwk: String?
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(AuthenticationClient::class.java)
    }

    init {
        logger.info("AzureClientIdSatt=${azureClientId != null}")
        logger.info("AzureClientSecretSatt=${azureClientSecret != null}")
        logger.info("AzureJwkSatt=${azureJwk != null}")
    }

}