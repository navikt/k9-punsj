package no.nav.k9punsj.sikkerhet.abac

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.GsonBuilder
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AppConfiguration
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.audit.*
import no.nav.k9punsj.sikkerhet.azuregraph.AzureGraphService
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.sikkerhet.audit.Auditdata
import no.nav.k9punsj.sikkerhet.audit.AuditdataHeader
import no.nav.k9punsj.sikkerhet.audit.Auditlogger
import no.nav.k9punsj.sikkerhet.audit.CefField
import no.nav.k9punsj.sikkerhet.audit.CefFieldName
import no.nav.k9punsj.sikkerhet.audit.EventClassId
import no.nav.k9punsj.utils.Cache
import no.nav.k9punsj.utils.CacheObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID


private val gson = GsonBuilder().setPrettyPrinting().create()

private const val XACML_CONTENT_TYPE = "application/xacml+json"
private const val DOMENE = "k9"

@Configuration
@StandardProfil
class PepClient(
    private val config: AppConfiguration,
    private val azureGraphService: AzureGraphService,
    private val auditlogger: Auditlogger,
    private val pdlService: PdlService,
) : IPepClient {

    private val url = config.abacEndpointUrl()
    private val log: Logger = LoggerFactory.getLogger(PepClient::class.java)
    private val cache = Cache<Boolean>()

    override suspend fun harBasisTilgang(fnr: String, urlKallet: String): Boolean {
        val identTilInnloggetBruker = azureGraphService.hentIdentTilInnloggetBruker()
        val requestBuilder = basisTilgangRequest(identTilInnloggetBruker, fnr)
        val decision = evaluate(requestBuilder)
        loggTilAudit(identTilInnloggetBruker, fnr, EventClassId.AUDIT_ACCESS, BASIS_TILGANG, "read", urlKallet)
        return decision
    }

    override suspend fun sendeInnTilgang(fnr: String, urlKallet: String): Boolean {
        val identTilInnloggetBruker = azureGraphService.hentIdentTilInnloggetBruker()
        val requestBuilder = sendeInnTilgangRequest(identTilInnloggetBruker, fnr)
        val decision = evaluate(requestBuilder)
        loggTilAudit(identTilInnloggetBruker, fnr, EventClassId.AUDIT_CREATE, TILGANG_SAK, "create", urlKallet)
        return decision
    }

    override suspend fun erSaksbehandler(): Boolean {
        val identTilInnloggetBruker = azureGraphService.hentIdentTilInnloggetBruker()
        val erSaksbehandlerIK9Sak = erSaksbehandlerIK9Sak(identTilInnloggetBruker)
        return evaluate(erSaksbehandlerIK9Sak)
    }

    private fun basisTilgangRequest(
        identTilInnloggetBruker: String,
        fnr: String,
    ): XacmlRequestBuilder {
        return XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, BASIS_TILGANG)
            .addActionAttribute(ACTION_ID, "read")
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, identTilInnloggetBruker)
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9punsj")
            .addResourceAttribute(RESOURCE_FNR, fnr)
    }

    private fun sendeInnTilgangRequest(
        identTilInnloggetBruker: String,
        fnr: String,
    ): XacmlRequestBuilder {
        return XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, TILGANG_SAK)
            .addActionAttribute(ACTION_ID, "create")
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, identTilInnloggetBruker)
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9punsj")
            .addResourceAttribute(RESOURCE_FNR, fnr)
    }

    private fun erSaksbehandlerIK9Sak(identTilInnloggetBruker: String): XacmlRequestBuilder {
        return XacmlRequestBuilder()
            .addResourceAttribute(RESOURCE_DOMENE, DOMENE)
            .addResourceAttribute(RESOURCE_TYPE, TILGANG_SAK)
            .addActionAttribute(ACTION_ID, "create")
            .addAccessSubjectAttribute(SUBJECT_TYPE, INTERNBRUKER)
            .addAccessSubjectAttribute(SUBJECTID, identTilInnloggetBruker)
            .addEnvironmentAttribute(ENVIRONMENT_PEP_ID, "srvk9los")
    }

    override suspend fun harBasisTilgang(fnr: List<String>, urlKallet: String): Boolean {
        val identTilInnloggetBruker = azureGraphService.hentIdentTilInnloggetBruker()

        fnr.forEach {
            loggTilAudit(identTilInnloggetBruker, it, EventClassId.AUDIT_ACCESS, BASIS_TILGANG, "read", urlKallet)
        }
        return fnr.map { basisTilgangRequest(identTilInnloggetBruker, it) }.map { evaluate(it) }.all { true }
    }

    override suspend fun sendeInnTilgang(fnr: List<String>, urlKallet: String): Boolean {
        val identTilInnloggetBruker = azureGraphService.hentIdentTilInnloggetBruker()

        fnr.forEach {
            loggTilAudit(identTilInnloggetBruker, it, EventClassId.AUDIT_ACCESS, TILGANG_SAK, "read", urlKallet)
        }
        return fnr.map { sendeInnTilgangRequest(identTilInnloggetBruker, it) }.map { evaluate(it) }.all { true }
    }

    private suspend fun loggTilAudit(
        identTilInnloggetBruker: String,
        it: String,
        eventClassId: EventClassId,
        type: String,
        action: String,
        url: String
    ) {
        auditlogger.logg(
            Auditdata(
                header = AuditdataHeader(
                    vendor = auditlogger.defaultVendor,
                    product = auditlogger.defaultProduct,
                    eventClassId = eventClassId,
                    name = "ABAC Sporingslogg",
                    severity = "INFO"
                ), fields = setOf(
                    CefField(CefFieldName.EVENT_TIME, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000L),
                    CefField(CefFieldName.REQUEST, url),
                    CefField(CefFieldName.ABAC_RESOURCE_TYPE, type),
                    CefField(CefFieldName.ABAC_ACTION, action),
                    CefField(CefFieldName.USER_ID, identTilInnloggetBruker),
                    CefField(CefFieldName.BERORT_BRUKER_ID, pdlService.aktørIdFor(it)),
                )
            )
        )
    }

    private suspend fun evaluate(xacmlRequestBuilder: XacmlRequestBuilder): Boolean {
        val xacmlJson = gson.toJson(xacmlRequestBuilder.build())
        val get = cache.get(xacmlJson)

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

            val result = try {
                objectMapper().readValue<Response>(response.body ?: "").response[0].decision == "Permit"
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
