package no.nav.k9.pdl

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.helsesjekk
import no.nav.k9.hentAuthentication
import no.nav.k9.hentCorrelationId
import no.nav.k9.journalpost.SafGateway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import kotlin.coroutines.coroutineContext

@Service
class PdlService (
        @Value("\${no.nav.pdl.base_url}") baseUrl: URI,
        @Qualifier("sts")private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator{

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val scope: Set<String> = setOf("openid")
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SafGateway::class.java)
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val NavConsumerTokenHeaderKey = "Nav-Consumer-Token"
        private const val TemaHeaderValue = "OMS"
        private const val TemaHeader = "Tema"
        private const val CorrelationIdHeader = "Nav-Callid"
        private const val MaxDokumentSize = 5 * 1024 * 1024
    }
    init {
        logger.info("PdlBaseUrl=$baseUrl")
        logger.info("PdlScopes=${scope.joinToString()}")
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
    suspend fun identifikator(fnummer: String): PdlResponse? {
        val accessToken = cachedAccessTokenClient
                .getAccessToken(
                        scopes = scope
                )
        val req =  QueryRequest(
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
                .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                .header(TemaHeader, TemaHeaderValue)
                .header(HttpHeaders.AUTHORIZATION, coroutineContext.hentAuthentication().accessToken)
                .header(NavConsumerTokenHeaderKey, accessToken.asAuthoriationHeader())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .toEntity(String::class.java)
                .awaitFirst()
        logger.info(response.toString())
        val aktøridPdl = response.body ?: return null
//        if (aktøridPdl.data == null) {
//            logger.info(objectMapper.writeValueAsString(aktøridPdl))
//            throw IkkeTilgang()
//        }
//        
        
        return PdlResponse(false, aktorId = AktøridPdl(data = AktøridPdl.Data(hentIdenter = null)))

    }

    private fun getStringFromResource(path: String) =
            PdlService::class.java.getResourceAsStream(path).bufferedReader().use { it.readText() }
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
                    operasjon = "hente-aktørid",
                    scopes = scope,
                    initialHealth = accessTokenClient.helsesjekk(
                            operasjon = "hente-aktørid",
                            scopes = scope
                    )
            )
    )

    internal class IkkeTilgang : Throwable("Saksbehandler har ikke tilgang til å slå opp personen.")
}