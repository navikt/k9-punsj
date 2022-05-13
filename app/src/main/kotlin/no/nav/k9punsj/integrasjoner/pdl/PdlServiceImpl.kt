package no.nav.k9punsj.integrasjoner.pdl

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.IkkeTestProfil
import no.nav.k9punsj.hentAuthentication
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.tilgangskontroll.helsesjekk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import kotlin.coroutines.coroutineContext

@Configuration
@IkkeTestProfil
class PdlServiceImpl(
    @Value("\${no.nav.pdl.base_url}") baseUrl: URI,
    @Value("\${no.nav.pdl.scope}") scope: String,
    @Qualifier("azure") private val azureAccessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator, PdlService {

    private val cachedAzureAccessTokenClient = CachedAccessTokenClient(azureAccessTokenClient)
    private val AzureScopes = setOf(scope)

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PdlServiceImpl::class.java)
        private const val TemaHeaderValue = "OMS"
        private const val TemaHeader = "Tema"
        private const val CorrelationIdHeader = "Nav-Callid"

        private fun getStringFromResource(path: String) =
            PdlServiceImpl::class.java.getResourceAsStream(path)!!.bufferedReader().use { it.readText() }

        private val HENT_IDENT =  getStringFromResource("/pdl/hentIdent.graphql")
        private val HENT_RELASJONER =  getStringFromResource("/pdl/hent-relasjoner.graphql")
        private val HENT_PERSONOPPLYSNINGER =  getStringFromResource("/pdl/hent-personopplysninger.graphql")
    }

    init {
        logger.info("PdlBaseUrl=$baseUrl")
        logger.info("PdlScopes=${AzureScopes.joinToString()}")
    }

    private val client = WebClient
        .builder()
        .baseUrl(baseUrl.toString())
        .build()

    @Throws(IkkeTilgang::class)
    override suspend fun identifikator(fnummer: String): PdlResponse? {
        val req = QueryRequest(
            query = HENT_IDENT,
            variables = mapOf(
                "ident" to fnummer,
                "historikk" to "false",
                "grupper" to listOf("AKTORID")
            )
        )
        val response = requestPdl(req)
        val (data, errors) = objectMapper().readValue<IdentPdl>(response)
        if (errors != null) {
            logger.warn(objectMapper().writeValueAsString(errors))
        }
        return PdlResponse(false, identPdl = IdentPdl(data, errors))

    }

    @Throws(IkkeTilgang::class)
    override suspend fun identifikatorMedAktørId(aktørId: String): PdlResponse? {
        val req = QueryRequest(
            query = HENT_IDENT,
            variables = mapOf(
                "ident" to aktørId,
                "historikk" to "false",
                "grupper" to listOf("FOLKEREGISTERIDENT")
            )
        )
        val response = requestPdl(req)
        val (data, errors) = objectMapper().readValue<IdentPdl>(response)
        if (errors != null) {
            logger.warn(objectMapper().writeValueAsString(errors))
        }
        return PdlResponse(false, identPdl = IdentPdl(data, errors))
    }

    @Throws(IkkeTilgang::class)
    override suspend fun aktørIdFor(fnummer: String): String? {
        val req = QueryRequest(
            query = HENT_IDENT,
            variables = mapOf(
                "ident" to fnummer,
                "historikk" to "false",
                "grupper" to listOf("AKTORID")
            )
        )
        val response = requestPdl(req)

        val (data, errors) = objectMapper().readValue<IdentPdl>(response)
        if (errors != null) {
            logger.warn(objectMapper().writeValueAsString(errors))
        }
        val pdlResponse = PdlResponse(false, identPdl = IdentPdl(data, errors))

        return pdlResponse.identPdl?.data?.hentIdenter?.identer?.first()?.ident
            ?: throw IllegalStateException("Fant ikke aktørId i PDL")
    }

    @Throws(IkkeTilgang::class)
    override suspend fun hentBarn(identitetsnummer: String): Set<String> {
        val request = QueryRequest(
            query = HENT_RELASJONER,
            variables = mapOf(
                "ident" to identitetsnummer
            )
        )

        return requestPdlJson(request).mapBarnFraRelasjoner()
    }

    @Throws(IkkeTilgang::class)
    override suspend fun hentPersonopplysninger(identitetsnummer: Set<String>): Set<Personopplysninger> {
        if (identitetsnummer.isEmpty()) {
            return emptySet()
        }

        val request = QueryRequest(
            query = HENT_PERSONOPPLYSNINGER,
            variables = mapOf(
                "identer" to identitetsnummer
            )
        )

        return requestPdlJson(
            queryRequest = request,
            getAuthorizationHeader = {systemAuthorizationHeader()}
        ).mapPersonopplysninger()
    }

    private suspend fun requestPdl(
        queryRequest: QueryRequest,
        getAuthorizationHeader: suspend () -> String = {userAuthorizationHeader()}) : String {
        val response = client
            .post()
            .uri { it.build() }
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(TemaHeader, TemaHeaderValue)
            .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader())
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(queryRequest)
            .retrieve()
            .toEntity(String::class.java)
            .awaitFirst()

        if (response.statusCode != HttpStatus.OK) {
            throw IllegalStateException("Uventet HTTP ${response.statusCodeValue} fra PDL. ResponseBody=${response.body}")
        }
        return requireNotNull(response.body) {
            "Uventet response fra PDL. Har ingen response body."
        }
    }

    private suspend fun requestPdlJson(
        queryRequest: QueryRequest,
        getAuthorizationHeader: suspend () -> String = {userAuthorizationHeader()}) : ObjectNode {
        val response = objectMapper().readTree(requestPdl(queryRequest, getAuthorizationHeader)) as ObjectNode

        val errors = when (response.hasNonNull("errors")) {
            true -> response.get("errors") as ArrayNode
            false -> objectMapper().createArrayNode()
        }

        if (errors.size() > 0) {
            logger.warn("Errors i response fra PDL. Errors=${objectMapper().writeValueAsString(errors)}")
        }

        return response
    }

    data class QueryRequest(
        val query: String,
        val variables: Map<String, Any>,
        val operationName: String? = null
    )

    private suspend fun userAuthorizationHeader() = cachedAzureAccessTokenClient.getAccessToken(
        scopes = AzureScopes,
        onBehalfOf = coroutineContext.hentAuthentication().accessToken
    ).asAuthoriationHeader()

    private fun systemAuthorizationHeader() = cachedAzureAccessTokenClient.getAccessToken(
        scopes = AzureScopes
    ).asAuthoriationHeader()

    override fun health() = Mono.just(
        azureAccessTokenClient.helsesjekk(
            operasjon = "pdl-integrasjon",
            scopes = AzureScopes,
            initialHealth = azureAccessTokenClient.helsesjekk(
                operasjon = "pdl-integrasjon",
                scopes = AzureScopes
            )
        )
    )

    private class IkkeTilgang : Throwable("Saksbehandler har ikke tilgang til å slå opp personen.")
}
