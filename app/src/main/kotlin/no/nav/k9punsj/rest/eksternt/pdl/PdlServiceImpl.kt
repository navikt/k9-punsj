package no.nav.k9punsj.rest.eksternt.pdl

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.helsesjekk
import no.nav.k9punsj.hentAuthentication
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.journalpost.SafGateway
import no.nav.k9punsj.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import kotlin.coroutines.coroutineContext

@Configuration
@Profile("!test")
class PdlServiceImpl(
    @Value("\${no.nav.pdl.base_url}") baseUrl: URI,
    @Value("\${no.nav.pdl.scope}") private val scope: String,
    @Qualifier("sts") private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator, PdlService {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val scopes: Set<String> = setOf(scope)

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SafGateway::class.java)
        private const val TemaHeaderValue = "OMS"
        private const val TemaHeader = "Tema"
        private const val CorrelationIdHeader = "Nav-Call-Id"
        private const val MaxDokumentSize = 5 * 1024 * 1024
    }

    init {
        logger.info("PdlBaseUrl=$baseUrl")
        logger.info("PdlScopes=${scopes.joinToString()}")
    }

    private val client = WebClient
        .builder()
        .baseUrl(baseUrl.toString())
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { configurer ->
                    configurer
                        .defaultCodecs()
                        .maxInMemorySize(MaxDokumentSize)
                }.build()
        )
        .build()

    @Throws(IkkeTilgang::class)
    override suspend fun identifikator(fnummer: String): PdlResponse? {
        val req = QueryRequest(
            getStringFromResource("/pdl/hentIdent.graphql"),
            mapOf(
                "ident" to fnummer,
                "historikk" to "false",
                "grupper" to listOf("AKTORID")
            )
        )
        val response = client
            .post()
            .uri { it.build() }
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(TemaHeader, TemaHeaderValue)
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .toEntity(String::class.java)
            .awaitFirst()

        val json = response.body ?: return null
        val (data, errors) = objectMapper().readValue<IdentPdl>(json)
        if (errors != null) {
            logger.warn(objectMapper().writeValueAsString(errors))
        }
        return PdlResponse(false, identPdl = IdentPdl(data, errors))

    }

    @Throws(IkkeTilgang::class)
    override suspend fun identifikatorMedAktørId(aktørId: String): PdlResponse? {
        val req = QueryRequest(
            getStringFromResource("/pdl/hentIdent.graphql"),
            mapOf(
                "ident" to aktørId,
                "historikk" to "false",
                "grupper" to listOf("FOLKEREGISTERIDENT")
            )
        )
        val response = client
            .post()
            .uri { it.build() }
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(TemaHeader, TemaHeaderValue)
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .toEntity(String::class.java)
            .awaitFirst()

        val json = response.body ?: return null
        val (data, errors) = objectMapper().readValue<IdentPdl>(json)
        if (errors != null) {
            logger.warn(objectMapper().writeValueAsString(errors))
        }
        return PdlResponse(false, identPdl = IdentPdl(data, errors))

    }

    @Throws(IkkeTilgang::class)
    override suspend fun aktørIdFor(fnummer: String): String? {
        val req = QueryRequest(
            getStringFromResource("/pdl/hentIdent.graphql"),
            mapOf(
                "ident" to fnummer,
                "historikk" to "false",
                "grupper" to listOf("AKTORID")
            )
        )
        val response = client
            .post()
            .uri { it.build() }
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(TemaHeader, TemaHeaderValue)
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .toEntity(String::class.java)
            .awaitFirst()

        val json = response.body ?: return null
        val (data, errors) = objectMapper().readValue<IdentPdl>(json)
        if (errors != null) {
            logger.warn(objectMapper().writeValueAsString(errors))
        }
        val pdlResponse = PdlResponse(false, identPdl = IdentPdl(data, errors))

        return pdlResponse.identPdl?.data?.hentIdenter?.identer?.first()?.ident
            ?: throw IllegalStateException("Fant ikke aktørId i PDL")
    }

    private fun getStringFromResource(path: String) =
        PdlServiceImpl::class.java.getResourceAsStream(path).bufferedReader().use { it.readText() }

    private suspend fun authorizationHeader() = cachedAccessTokenClient.getAccessToken(
        scopes = scopes,
        onBehalfOf = coroutineContext.hentAuthentication().accessToken
    ).asAuthoriationHeader()

    data class QueryRequest(
        val query: String,
        val variables: Map<String, Any>,
        val operationName: String? = null
    ) {
        data class Variables(
            val variables: Map<String, Any>
        )
    }

    override fun health() = Mono.just(
        accessTokenClient.helsesjekk(
            operasjon = "pdl-integrasjon",
            scopes = scopes,
            initialHealth = accessTokenClient.helsesjekk(
                operasjon = "pdl-integrasjon",
                scopes = scopes
            )
        )
    )

    internal class IkkeTilgang : Throwable("Saksbehandler har ikke tilgang til å slå opp personen.")
}
