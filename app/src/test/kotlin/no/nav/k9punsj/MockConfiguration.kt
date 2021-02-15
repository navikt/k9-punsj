package no.nav.k9punsj

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV1WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.k9punsj.wiremock.getK9sakBaseUrl
import no.nav.k9punsj.wiremock.getPdlBaseUrl
import no.nav.k9punsj.wiremock.getSafBaseUrl

internal object MockConfiguration {
    internal fun config(
            wireMockServer: WireMockServer,
            port: Int,
            azureV2DiscoveryUrl: String?,
    ): Map<String, String> = mapOf(
        "PORT" to "$port",
        "AZURE_client_id" to "k9-punsj",
        "AZURE_jwk" to ClientCredentials.ClientA.privateKeyJwk,
        "AZURE_token_endpoint" to wireMockServer.getAzureV2TokenUrl(),
        "AZURE_V1_discovery_url" to wireMockServer.getAzureV1WellKnownUrl(),
//        "AZURE_V2_discovery_url" to "http://azure-mock:8100/v2.0/.well-known/openid-configuration",
        "AZURE_V2_discovery_url" to (azureV2DiscoveryUrl ?: wireMockServer.getAzureV2WellKnownUrl()),
        "SYSTEMBRUKER_USERNAME" to "vtp",
        "SYSTEMBRUKER_PASSWORD" to "vtp",
        "NAV_TRUSTSTORE_PATH" to "${System.getProperty("user.home")}/.modig/truststore.jks",
        "NAV_TRUSTSTORE_PASSWORD" to "changeit",
        "SAF_BASE_URL" to wireMockServer.getSafBaseUrl(),
        "PDL_BASE_URL" to wireMockServer.getPdlBaseUrl(),
        "K9SAK_BASE_URL" to wireMockServer.getK9sakBaseUrl(),
        "GOSYS_BASE_URL" to wireMockServer.getPdlBaseUrl(),
        "SAF_HENTE_JOURNALPOST_SCOPES" to "saf-client-id/.default",
        "SAF_HENTE_DOKUMENT_SCOPES" to "saf-client-id/.default",
        "SWAGGER_SERVER_BASE_URL" to "http://localhost:$port",
        "KAFKA_BOOTSTRAP_SERVERS" to "localhost:9093",
        "DEFAULTDS_USERNAME" to "k9punsj_unit",
        "DEFAULTDS_PASSWORD" to "k9punsj_unit",
        "DEFAULTDS_URL" to "jdbc:postgresql://127.0.0.1:5432/k9punsj_unit",
        "DEFAULTDS_VAULT_MOUNTPATH" to "",
        "NAIS_STS_TOKEN_ENDPOINT" to wireMockServer.getNaisStsTokenUrl()
    )
}
