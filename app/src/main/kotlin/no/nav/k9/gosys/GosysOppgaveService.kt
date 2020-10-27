package no.nav.k9.gosys

import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.hentCorrelationId
import no.nav.k9.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

@Service
class GosysOppgaveService(
        @Value("\${no.nav.gosys.base_url}") safBaseUrl: URI,
        @Qualifier("sts") private val accessTokenClient: AccessTokenClient
) {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(GosysOppgaveService::class.java)
        private const val VariantType = "ARKIV"
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "Nav-Callid"
        private val scope: Set<String> = setOf("openid")
        private const val MaxDokumentSize = 5 * 1024 * 1024
    }

    private val client = WebClient
            .builder()
            .baseUrl(safBaseUrl.toString())
            .exchangeStrategies(
                    ExchangeStrategies.builder()
                            .codecs { configurer ->
                                configurer
                                        .defaultCodecs()
                                        .maxInMemorySize(MaxDokumentSize)
                            }.build()
            )
            .build()

    suspend fun opprettOppgave(aktørid: String, joarnalpostId: String) {
        val accessToken = cachedAccessTokenClient
                .getAccessToken(
                        scopes = scope
                )
        val opprettOppgaveRequest = OpprettOppgaveRequest(aktivDato = LocalDate.now(),
                aktoerId = aktørid,
                journalpostId = joarnalpostId,
                oppgavetype = "JFR",
                prioritet = Prioritet.NORM,
                tema = "OMS")
        try {
            val response = client
                    .post()
                    .uri { it.pathSegment("api", "v1", "oppgaver").build() }
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
                    .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                    .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                    .bodyValue(objectMapper().writeValueAsString(opprettOppgaveRequest))
                    .exchange().flatMap { clientResponse ->
                        logger.error(clientResponse.toString())
                        clientResponse.body { clientHttpResponse, context -> clientHttpResponse.body }
                        clientResponse.bodyToMono(String::class.java)
                    }
            logger.info(response.toString())

        } catch (e: Exception) {
            logger.error("", e)
        }
    }


}