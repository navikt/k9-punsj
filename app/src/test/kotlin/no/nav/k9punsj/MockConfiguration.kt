package no.nav.k9punsj

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsWellKnownUrl
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.wiremock.getAaregBaseUrl
import no.nav.k9punsj.wiremock.getDokarkivBaseUrl
import no.nav.k9punsj.wiremock.getEregBaseUrl
import no.nav.k9punsj.wiremock.getGosysBaseUrl
import no.nav.k9punsj.wiremock.getK9PunsjbolleBaseUrl
import no.nav.k9punsj.wiremock.getK9sakBaseUrl
import no.nav.k9punsj.wiremock.getPdlBaseUrl
import no.nav.k9punsj.wiremock.getSafBaseUrl
import java.net.URI

internal object MockConfiguration {
    internal fun config(
        wireMockServer: WireMockServer,
        port: Int,
        azureV2Url: URI?
    ): Map<String, String> {
        val (wellKnownUrl, tokenUrl) = when (azureV2Url) {
            null -> wireMockServer.getAzureV2WellKnownUrl() to wireMockServer.getAzureV2TokenUrl()
            else -> "$azureV2Url/.well-known/openid-configuration" to "$azureV2Url/token"
        }

        return mapOf(
            "PORT" to "$port",
            "AZURE_APP_CLIENT_ID" to "k9-punsj",
            "AZURE_APP_JWK" to ClientCredentials.ClientA.privateKeyJwk,
            "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to tokenUrl,
            "AZURE_APP_WELL_KNOWN_URL" to wellKnownUrl,
            "SYSTEMBRUKER_USERNAME" to "vtp",
            "SYSTEMBRUKER_PASSWORD" to "vtp",
            "NAV_TRUSTSTORE_PATH" to "${System.getProperty("user.home")}/.modig/truststore.jks",
            "NAV_TRUSTSTORE_PASSWORD" to "changeit",
            "SAF_BASE_URL" to wireMockServer.getSafBaseUrl(),
            "DOKARKIV_BASE_URL" to wireMockServer.getDokarkivBaseUrl(),
            "PDL_BASE_URL" to wireMockServer.getPdlBaseUrl(),
            "PDL_SCOPE" to "pdl/.default",
            "DOKARKIV_SCOPE" to "dokarkiv/.default",
            "K9SAK_BASE_URL" to wireMockServer.getK9sakBaseUrl(),
            "GOSYS_BASE_URL" to wireMockServer.getGosysBaseUrl(),
            "GOSYS_BASE_SCOPE" to "gosys/.default",
            "SAF_HENTE_JOURNALPOST_SCOPES" to "saf-client-id/.default",
            "SAF_HENTE_DOKUMENT_SCOPES" to "saf-client-id/.default",
            "SWAGGER_SERVER_BASE_URL" to "http://localhost:$port",
            "KAFKA_BOOTSTRAP_SERVERS" to "localhost:9093",
            "DEFAULTDS_USERNAME" to "postgres",
            "DEFAULTDS_PASSWORD" to "postgres",
            "DEFAULTDS_URL" to "jdbc:postgresql://localhost:${DatabaseUtil.embeddedPostgres.port}/postgres",
            "DEFAULTDS_VAULT_MOUNTPATH" to "",
            "NAIS_STS_TOKEN_ENDPOINT" to wireMockServer.getNaisStsTokenUrl(),
            "AUDITLOGGER_ENABLED" to "false",
            "ABAC_PDP_ENDPOINT_URL" to "",
            "AUDITLOGGER_VENDOR" to "",
            "AUDITLOGGER_PRODUCT" to "",
            "K9PUNSJBOLLE_BASE_URL" to wireMockServer.getK9PunsjbolleBaseUrl(),
            "K9PUNSJBOLLE_SCOPE" to "k9-punsjbolle-id/.default",
            "APP_NAISSTS_aud" to "srvk9sak",
            "APP_NAISSTS_discovery_url" to wireMockServer.getNaisStsWellKnownUrl(),
            "AAREG_BASE_URL" to wireMockServer.getAaregBaseUrl(),
            "AAREG_SCOPE" to "aareg-services-nais/.default",
            "EREG_BASE_URL" to wireMockServer.getEregBaseUrl(),
            "SEND_BREVBESTILLING_TIL_K9_FORMIDLING" to "privat-k9-dokumenthendelse",
            "SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS" to "privat-k9punsj-aksjonspunkthendelse-v1",
            "NOTAT_ENABLED" to "true"
        )
    }
}
