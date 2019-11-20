package no.nav.k9

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV1WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.k9.wiremock.getSafBaseUrl
import java.lang.System.setProperty

internal object MockConfiguration {
    internal fun config(
            wireMockServer: WireMockServer
    ) = mapOf(
            "AZURE_client_id" to "k9-punsj",
            "AZURE_jwk" to ClientCredentials.ClientA.privateKeyJwk,
            "AZURE_token_endpoint" to wireMockServer.getAzureV2TokenUrl(),
            "AZURE_V1_discovery_url" to wireMockServer.getAzureV1WellKnownUrl(),
            "AZURE_V2_discovery_url" to wireMockServer.getAzureV2WellKnownUrl(),
            "SAF_BASE_URL" to wireMockServer.getSafBaseUrl(),
            "SAF_HENTE_JOURNALPOST_SCOPES" to "saf-client-id/.default",
            "SAF_HENTE_DOKUMENT_SCOPES" to "saf-client-id/.default"
    )
}

internal fun Map<String, String>.setAsProperties() = forEach { key, value ->
    setProperty(key, value)
}