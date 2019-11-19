package no.nav.k9

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.ktor.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.getAzureV1WellKnownUrl
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.oauth2.client.FromCertificateHexThumbprint
import no.nav.k9.wiremock.getSafBaseUrl
import org.json.JSONObject
import java.lang.System.setProperty
import java.net.URL

internal object MockConfiguration {
    internal fun config(
            wireMockServer: WireMockServer
    ) = mapOf(
            "AZURE_client_id" to "k9-punsj",
            "AZURE_jwk" to jwkMedKid(),
            "AZURE_token_endpoint" to wireMockServer.getAzureV2WellKnownUrl().tokenEndpoint(),
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

private fun String.tokenEndpoint() = JSONObject(URL(this).readText()).getString("token_endpoint")
private fun jwkMedKid() : String {
    val json = JSONObject(ClientCredentials.ClientA.privateKeyJwk)
    json.put("kid", FromCertificateHexThumbprint(ClientCredentials.ClientA.certificateHexThumbprint).getKeyId())
    return json.toString()
}