package no.nav.k9punsj.integrasjoner.gosys

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.felles.NavHeaders
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.integrasjoner.gosys.OppgaveGateway.Urls.oppgaveUrl
import no.nav.k9punsj.integrasjoner.gosys.OppgaveGateway.Urls.patchEksisterendeOppgaveUrl
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.utils.WebClienttUtils.håndterFeil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import java.net.URI
import java.util.*
import kotlin.coroutines.coroutineContext

/**
 * @see <a href="https://oppgave.dev.intern.nav.no/"> Se swagger definisjon for mer info</a>
 */
@Service
internal class OppgaveGateway(
    @Value("\${no.nav.gosys.base_url}") private val oppgaveBaseUrl: URI,
    @Value("\${no.nav.gosys.scope}") private val oppgaveScope: String,
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient,
) {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val client = WebClient
        .builder()
        .baseUrl(oppgaveBaseUrl.toString())
        .build()

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(OppgaveGateway::class.java)
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "X-Correlation-ID"
    }

    private object Urls {
        const val oppgaveUrl = "/api/v1/oppgaver"
        const val patchEksisterendeOppgaveUrl = "/api/v1/oppgaver"
    }

    suspend fun hentOppgave(oppgaveId: String): Triple<HttpStatusCode, String?, GetOppgaveResponse?> {

        val (url, response, responseBody) = httpGet("$oppgaveUrl/$oppgaveId")
        val harFeil = !response.statusCode.is2xxSuccessful
        val httpStatus = response.statusCode
        if (harFeil) {
            logger.error("Feil ved henting av oppgave. Url=[$url], HttpStatus=[$httpStatus], Response=$responseBody")
        }

        return Triple(
            httpStatus,
            if (harFeil) "Feil ved henting av oppgave. Url=[$url], HttpStatus=[$httpStatus], Response=$responseBody" else null,
            if (!harFeil) objectMapper().readValue(responseBody, GetOppgaveResponse::class.java) else null
        )
    }

    /**
     * Oppretter en ny oppgave.
     *
     * @see <a href="Oppretter en ny oppgave">Oppretter en ny oppgave</a>
     */
    suspend fun opprettOppgave(aktørid: String, joarnalpostId: String, gjelder: Gjelder): Pair<HttpStatusCode, String?> {
        val opprettOppgaveRequest = OpprettOppgaveRequest(
            aktoerId = aktørid,
            journalpostId = joarnalpostId,
            gjelder = gjelder
        )

        val body = kotlin.runCatching { objectMapper().writeValueAsString(opprettOppgaveRequest) }
            .getOrElse {
                logger.error(it.message)
                throw it
            }

        val (url, response, responseBody) = httpPost(body, oppgaveUrl)

        val httpStatus = response.statusCode
        val harFeil = !httpStatus.is2xxSuccessful
        if (harFeil) {
            logger.error("Feil ved opprettOppgaveGosysoppgave. Url=[$url], HttpStatus=[$httpStatus], Response=$responseBody")
        }
        return httpStatus to if (harFeil) "Feil ved opprettOppgaveGosysoppgave. Url=[$url], HttpStatus=[$httpStatus], Response=$responseBody" else null
    }

    /**
     * Endrer eksisterende oppgave.
     *
     * Denne operasjonen endrer kun på verdier som er gitt. Felter som ikke er med vil ikke bli berørt.
     * @see <a href="https://oppgave.dev.intern.nav.no/#/Oppgave/patchOppgave">Endre eksisterende oppgave</a>
     */
    suspend fun patchOppgave(oppgaveId: String, patchOppgaveRequest: PatchOppgaveRequest): Pair<HttpStatusCode, String?> {
        val body = kotlin.runCatching { objectMapper().writeValueAsString(patchOppgaveRequest) }
            .getOrElse {
                logger.error(it.message)
                throw it
            }

        val (url, response, responseBody) = httpPatch(body, "$patchEksisterendeOppgaveUrl/$oppgaveId")

        val httpStatus = response.statusCode
        val harFeil = !httpStatus.is2xxSuccessful
        if (harFeil) {
            logger.error("Feil ved endring av gosysoppgave. Url=[$url], HttpStatus=[$httpStatus], Response=$responseBody")
        }
        return httpStatus to if (harFeil) "Feil ved endring av gosysoppgave. Url=[$url], HttpStatus=[$httpStatus], Response=$responseBody" else null
    }

    private suspend fun httpPost(body: String, url: String): Triple<String, ResponseEntity<String>, String?> {
        val responseEntity = kotlin.runCatching {
            client
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, cachedAccessTokenClient.getAccessToken(setOf(oppgaveScope)).asAuthoriationHeader())
                .header(NavHeaders.CallId, UUID.randomUUID().toString())
                .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .toEntity(String::class.java)
                .awaitFirst()
        }.håndterFeil()

        return responseEntity.resolve(url)
    }

    private suspend fun httpGet(url: String): Triple<String, ResponseEntity<String>, String?> {
        val responseEntity = kotlin.runCatching {
            client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, cachedAccessTokenClient.getAccessToken(setOf(oppgaveScope)).asAuthoriationHeader())
                .header(NavHeaders.CallId, UUID.randomUUID().toString())
                .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String::class.java)
                .awaitFirst()
        }.håndterFeil()

        return responseEntity.resolve(url)
    }

    private suspend fun httpPatch(body: String, url: String): Triple<String, ResponseEntity<String>, String?> {
        val responseEntity: ResponseEntity<String> = kotlin.runCatching {
            client
                .patch()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, cachedAccessTokenClient.getAccessToken(setOf(oppgaveScope)).asAuthoriationHeader())
                .header(NavHeaders.CallId, UUID.randomUUID().toString())
                .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .toEntity<String>()
                .awaitFirst()
        }.håndterFeil()

        return responseEntity.resolve(url)
    }

    private fun ResponseEntity<String>.resolve(url: String): Triple<String, ResponseEntity<String>, String?> = when {
        !statusCode.is2xxSuccessful -> {
            when {
                body.isNullOrBlank() -> {
                    Triple(url, this, "{}")
                }
                else -> {
                    Triple(url, this, body!!)
                }
            }
        }
        else -> {
            Triple(url, this, body)
        }
    }
}
