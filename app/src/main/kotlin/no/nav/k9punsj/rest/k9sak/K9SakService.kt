package no.nav.k9punsj.rest.k9sak

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.helsesjekk
import no.nav.k9punsj.journalpost.SafGateway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI

@Service
class K9SakService(
        @Value("\${no.nav.pdl.base_url}") baseUrl: URI,
        @Qualifier("sts") private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator {

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
    suspend fun hentSaksnummer(fnummer: String): PdlResponse? {
      return null

    }

    private fun getStringFromResource(path: String) =
            K9SakService::class.java.getResourceAsStream(path).bufferedReader().use { it.readText() }

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
