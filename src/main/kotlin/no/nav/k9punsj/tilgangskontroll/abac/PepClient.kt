package no.nav.k9punsj.tilgangskontroll.abac

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.GsonBuilder
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.configuration.AuditConfiguration
import no.nav.k9punsj.idToken
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.tilgangskontroll.audit.*
import no.nav.k9punsj.utils.Cache
import no.nav.k9punsj.utils.CacheObject
import no.nav.k9punsj.utils.objectMapper
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.sif.abac.kontrakt.person.PersonIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.coroutines.coroutineContext

private val gson = GsonBuilder().setPrettyPrinting().create()

private const val XACML_CONTENT_TYPE = "application/xacml+json"
private const val DOMENE = "k9"

@Configuration
@StandardProfil
class PepClient(
    private val config: AuditConfiguration,
    private val auditlogger: Auditlogger,
    private val pdlService: PdlService,
    private val sifAbacPdpKlient: SifAbacPdpKlient,
    @Value("\${valgt.pdp}") private val valgtPdp: String,
) : IPepClient {

    private val url = config.abacEndpointUrl()
    private val log: Logger = LoggerFactory.getLogger(PepClient::class.java)
    private val cache = Cache<Boolean>()

    override suspend fun harBasisTilgang(fnr: String, urlKallet: String): Boolean {
        return velgImpl(
            abacK9Impl = {
                val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
                val requestBuilder = basisTilgangRequest(identTilInnloggetBruker, fnr)
                val decision = evaluate(requestBuilder)
                loggTilAudit(identTilInnloggetBruker, fnr, EventClassId.AUDIT_ACCESS, BASIS_TILGANG, "read", urlKallet)
                decision
            },
            sifAbacPdpImpl = {
                sifAbacPdpKlient.harTilgangTilPersoner(BeskyttetRessursActionAttributt.READ,  listOf(PersonIdent(fnr)))
                    .also {
                        val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
                        loggTilAudit(identTilInnloggetBruker, fnr, EventClassId.AUDIT_ACCESS, BASIS_TILGANG, "read", urlKallet)
                    }
            }
        )
    }

    override suspend fun harBasisTilgang(fnr: List<String>, urlKallet: String): Boolean {
        return velgImpl(
            abacK9Impl = {
                val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
                fnr.forEach {
                    loggTilAudit(identTilInnloggetBruker, it, EventClassId.AUDIT_ACCESS, BASIS_TILGANG, "read", urlKallet)
                }
                fnr.map { basisTilgangRequest(identTilInnloggetBruker, it) }.map { evaluate(it) }.all { true }
            },
            sifAbacPdpImpl = {
                sifAbacPdpKlient.harTilgangTilPersoner(BeskyttetRessursActionAttributt.READ, fnr.map { PersonIdent(it) })
                    .also {
                        val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
                        fnr.forEach {
                            loggTilAudit(identTilInnloggetBruker, it, EventClassId.AUDIT_ACCESS, BASIS_TILGANG, "read", urlKallet)
                        }
                    }
            }
        )
    }

    override suspend fun sendeInnTilgang(fnr: String, urlKallet: String): Boolean {
        return velgImpl(
            abacK9Impl = {
                val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
                val requestBuilder = sendeInnTilgangRequest(identTilInnloggetBruker, fnr)
                val decision = evaluate(requestBuilder)
                loggTilAudit(identTilInnloggetBruker, fnr, EventClassId.AUDIT_CREATE, TILGANG_SAK, "create", urlKallet)
                decision
            },
            sifAbacPdpImpl = {
                sifAbacPdpKlient.harTilgangTilPersoner(BeskyttetRessursActionAttributt.CREATE,  listOf(PersonIdent(fnr)))
                    .also {
                        val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
                        loggTilAudit(identTilInnloggetBruker, fnr, EventClassId.AUDIT_CREATE, TILGANG_SAK, "create", urlKallet)
                    }
            }
        )
    }

    override suspend fun sendeInnTilgang(fnr: List<String>, urlKallet: String): Boolean {
        return velgImpl(
            abacK9Impl = {
                val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
                fnr.forEach {
                    loggTilAudit(identTilInnloggetBruker, it, EventClassId.AUDIT_ACCESS, TILGANG_SAK, "read", urlKallet)
                }
                fnr.map { sendeInnTilgangRequest(identTilInnloggetBruker, it) }.map { evaluate(it) }.all { true }
            },
            sifAbacPdpImpl = {
                sifAbacPdpKlient.harTilgangTilPersoner(BeskyttetRessursActionAttributt.CREATE,  fnr.map { PersonIdent(it)})
                    .also {
                        val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
                        fnr.forEach {
                            loggTilAudit(identTilInnloggetBruker, it, EventClassId.AUDIT_CREATE, TILGANG_SAK, "create", urlKallet)
                        }
                    }
            }
        )
    }

    override suspend fun erSaksbehandler(): Boolean {
        return coroutineContext.idToken().erSaksbehandler()
    }

    private fun basisTilgangRequest(
        identTilInnloggetBruker: String,
        fnr: String
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
        fnr: String
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
                ),
                fields = setOf(
                    CefField(CefFieldName.EVENT_TIME, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000L),
                    CefField(CefFieldName.REQUEST, url),
                    CefField(CefFieldName.ABAC_RESOURCE_TYPE, type),
                    CefField(CefFieldName.ABAC_ACTION, action),
                    CefField(CefFieldName.USER_ID, identTilInnloggetBruker),
                    CefField(CefFieldName.BERORT_BRUKER_ID, pdlService.aktørIdFor(it))
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
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(xacmlJson)
                .retrieve()
                .toEntity(String::class.java)
                .awaitFirst()

            val result = try {
                objectMapper().readValue<Response>(response.body ?: "").response[0].decision == "Permit"
            } catch (e: Exception) {
                log.error(
                    "Feilet deserialisering",
                    e
                )
                false
            }
            cache.set(xacmlJson, CacheObject(result, LocalDateTime.now().plusHours(1)))
            return result
        } else {
            return get.value
        }
    }

    suspend fun <T> velgImpl(abacK9Impl: suspend () -> T, sifAbacPdpImpl: suspend () -> T): T {
        return when (valgtPdp) {
            "abac-k9" -> abacK9Impl()
            "sif-abac-pdp" -> sifAbacPdpImpl()
            "begge" -> {
                val resultat = abacK9Impl()
                try {
                    val resultatNy = sifAbacPdpImpl()
                    if (resultatNy != resultat) {
                        log.warn(
                            "Differanse i tilgangsjekk. Ny {} gammel {}. Bruker resultat for gammel sjekk",
                            resultatNy,
                            resultat
                        )
                    }
                } catch (e: Exception) {
                    log.warn("Feil i ny tilgangssjekk. Bruker resultat fra gammel. ", e)
                }
                resultat
            }
            else -> throw IllegalArgumentException("Ikke-støttet valgt pdp: " + valgtPdp)
        }
    }
}
