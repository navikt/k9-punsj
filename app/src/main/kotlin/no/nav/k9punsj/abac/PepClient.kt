package no.nav.k9punsj.abac

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.GsonBuilder
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AppConfiguration
import no.nav.k9punsj.azuregraph.AzureGraphService
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.utils.Cache
import no.nav.k9punsj.utils.CacheObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime
import java.util.UUID


private val gson = GsonBuilder().setPrettyPrinting().create()

private const val XACML_CONTENT_TYPE = "application/xacml+json"
private const val DOMENE = "k9"

@Configuration
@Profile("!test & !local")
class PepClient(private val config: AppConfiguration,
                private val azureGraphService: AzureGraphService): IPepClient {

    private val url = config.abacEndpointUrl()
    private val log: Logger = LoggerFactory.getLogger(PepClient::class.java)
    private val cache = Cache<Boolean>()

    override suspend fun harBasisTilgang(fnr: String): Boolean {
        val identTilInnloggetBruker = azureGraphService.hentIdentTilInnloggetBruker()
        val requestBuilder = XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, BASIS_TILGANG)
            .addActionAttribute(ACTION_ID, "read")
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, identTilInnloggetBruker)
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9los")
            .addResourceAttribute(RESOURCE_FNR, fnr)

        val decision = evaluate(requestBuilder)

        /*  auditlogger.logg(
              Auditdata(
                  header = AuditdataHeader(
                      vendor = auditlogger.defaultVendor,
                      product = auditlogger.defaultProduct,
                      eventClassId = EventClassId.AUDIT_SEARCH,
                      name = "ABAC Sporingslogg",
                      severity = "INFO"
                  ), fields = setOf(
                      CefField(CefFieldName.EVENT_TIME, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000L),
                      CefField(CefFieldName.REQUEST, "read"),
                      CefField(CefFieldName.ABAC_RESOURCE_TYPE, TILGANG_SAK),
                      CefField(CefFieldName.ABAC_ACTION, "read"),
                      CefField(CefFieldName.USER_ID, identTilInnloggetBruker),
                      CefField(CefFieldName.BERORT_BRUKER_ID, aktørid),
                  )
              )
          ) */

        return decision
    }

    private suspend fun evaluate(xacmlRequestBuilder: XacmlRequestBuilder): Boolean {
        val xacmlJson = gson.toJson(xacmlRequestBuilder.build())
        val get = cache.get(xacmlJson)
        var result = false
        if (get == null) {
            val client: WebClient = WebClient.builder()
                .filter(basicAuthentication(config.abacUsername(), config.abacPassword()))
                .build()

            val response = client
                .post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, XACML_CONTENT_TYPE)
                .header(NavHeaders.CallId, UUID.randomUUID().toString())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(xacmlJson)
                .retrieve()
                .toEntity(String::class.java)
                .awaitFirst()

            try {
                val json = response.body ?: ""
                result = objectMapper().readValue<Response>(json).response[0].decision == "Permit"
                return result
            } catch (e: Exception) {
                log.error(
                    "Feilet deserialisering", e
                )
                false
            }
            cache.set(xacmlJson, CacheObject(result, LocalDateTime.now().plusHours(1)))
            return result

        } else {
            return get.value
        }
    }
}
