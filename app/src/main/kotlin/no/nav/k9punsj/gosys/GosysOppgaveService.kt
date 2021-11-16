package no.nav.k9punsj.gosys

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import kotlin.coroutines.coroutineContext

@Service
internal class GosysOppgaveService(
        @Value("\${no.nav.gosys.base_url}") safBaseUrl: URI,
        @Qualifier("sts") private val accessTokenClient: AccessTokenClient) {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(GosysOppgaveService::class.java)
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "X-Correlation-ID"
        private val scope: Set<String> = setOf("openid")
    }

    private val client = WebClient
            .builder()
            .baseUrl(safBaseUrl.toString())
            .build()

    suspend fun opprettOppgave(aktørid: String, joarnalpostId: String, gjelder: Gjelder): Pair<HttpStatus, String?> {
        val accessToken = cachedAccessTokenClient.getAccessToken(
            scopes = scope
        )
        val opprettOppgaveRequest = OpprettOppgaveRequest(
            aktoerId = aktørid,
            journalpostId = joarnalpostId,
            gjelder = gjelder
        )
        try {
            val body = objectMapper().writeValueAsString(opprettOppgaveRequest)
            val response = client
                    .post()
                    .uri { it.pathSegment("api", "v1", "oppgaver").build() }
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
                    .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                    .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                    .bodyValue(body)

                    .retrieve()
                    .onStatus(HttpStatus::is4xxClientError) { clientResponse ->
                        clientResponse.bodyToFlux<String>().flatMap { it ->
                            logger.info(it)
                            Mono.just(it)
                        }
                        return@onStatus Mono.error(IllegalStateException())
                    }
                    .toEntity(String::class.java)
                    .awaitFirst()
            return Pair<HttpStatus, String?>(response.statusCode, null)

        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val exceptionAsString = sw.toString()
            return Pair(HttpStatus.INTERNAL_SERVER_ERROR, exceptionAsString)
        }
    }
}
