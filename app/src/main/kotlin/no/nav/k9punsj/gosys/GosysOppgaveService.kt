package no.nav.k9punsj.gosys

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.hentAuthentication
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
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono
import java.net.URI
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.coroutineContext


@Service
class GosysOppgaveService(
        @Value("\${no.nav.gosys.base_url}") gosysBaseUrl: URI,
        @Qualifier("sts") private val accessTokenClient: AccessTokenClient
) {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(GosysOppgaveService::class.java)
        private const val VariantType = "ARKIV"
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "X-Correlation-ID"
        private val scope: Set<String> = setOf("openid")
        private const val MaxDokumentSize = 5 * 1024 * 1024
    }

    private val client = WebClient
            .builder()
            .baseUrl(gosysBaseUrl.toString())
            .exchangeStrategies(
                    ExchangeStrategies.builder()
                            .codecs { configurer ->
                                configurer
                                        .defaultCodecs()
                                        .maxInMemorySize(MaxDokumentSize)
                            }.build()
            )
            .build()

    val url = "$gosysBaseUrl/api/v1/oppgaver"

    suspend fun opprettOppgave(aktørid: String, joarnalpostId: String) {

        val accessToken = cachedAccessTokenClient
                .getAccessToken(
                        scopes = scope
                )
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val opprettOppgaveRequest = OpprettOppgaveRequest(aktivDato = df.format(Date()),
                aktoerId = aktørid,
                journalpostId = joarnalpostId,
                oppgavetype = "JFR",
                prioritet = Prioritet.NORM,
                tema = "OMS")
        try {
            val body = objectMapper().writeValueAsString(opprettOppgaveRequest)
            logger.info("Sender request for å opprette journalføringsoppgave: $body")
            val (request, _, result) = url
                .httpPost()
                .body(body)
                .header(
                    HttpHeaders.ACCEPT to "application/json",
                    HttpHeaders.AUTHORIZATION to accessToken.asAuthoriationHeader(),
                    CorrelationIdHeader to coroutineContext.hentCorrelationId(),
                    ConsumerIdHeaderKey to ConsumerIdHeaderValue,
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON
                ).awaitStringResponseResult()

            result.fold(
                { success -> success },
                { error ->
                    logger.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved opprettelse av Gosys-oppgave")
                }
            )
        } catch (e: Exception) {
            logger.error("", e)
        }
    }
}
