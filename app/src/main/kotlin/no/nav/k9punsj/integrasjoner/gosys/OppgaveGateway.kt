package no.nav.k9punsj.integrasjoner.gosys

import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPatch
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.felles.NavHeaders
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.integrasjoner.gosys.OppgaveGateway.Urls.oppgaveUrl
import no.nav.k9punsj.integrasjoner.gosys.OppgaveGateway.Urls.patchEksisterendeOppgaveUrl
import no.nav.k9punsj.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.net.URI
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * @see <a href="https://oppgave.dev.intern.nav.no/"> Se swagger definisjon for mer info</a>
 */
@Service
internal class OppgaveGateway(
    @Value("\${no.nav.gosys.base_url}") private val oppgaveBaseUrl: URI,
    @Qualifier("sts") private val accessTokenClient: AccessTokenClient
) {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(OppgaveGateway::class.java)
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "X-Correlation-ID"
        private val scope: Set<String> = setOf("openid")
    }

    private object Urls {
        const val oppgaveUrl = "/api/v1/oppgaver"
        const val patchEksisterendeOppgaveUrl = "/api/v1/oppgaver"
    }

    /**
     * Oppretter en ny oppgave.
     *
     * @see <a href="Oppretter en ny oppgave">Oppretter en ny oppgave</a>
     */
    suspend fun opprettOppgave(aktørid: String, joarnalpostId: String, gjelder: Gjelder): Pair<HttpStatus, String?> {
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

        val harFeil = !response.isSuccessful
        if (harFeil) {
            logger.error("Feil ved opprettOppgaveGosysoppgave. Url=[$url], HttpStatus=[${response.statusCode}], Response=$responseBody")
        }
        return HttpStatus.valueOf(response.statusCode) to if (harFeil) "Feil ved opprettOppgaveGosysoppgave. Url=[$url], HttpStatus=[${response.statusCode}], Response=$responseBody" else null
    }

    /**
     * Endrer eksisterende oppgave.
     *
     * Denne operasjonen endrer kun på verdier som er gitt. Felter som ikke er med vil ikke bli berørt.
     * @see <a href="https://oppgave.dev.intern.nav.no/#/Oppgave/patchOppgave">Endre eksisterende oppgave</a>
     */
    suspend fun patchOppgave(oppgaveId: String, patchOppgaveRequest: PatchOppgaveRequest): Pair<HttpStatus, String?> {
        val body = kotlin.runCatching { objectMapper().writeValueAsString(patchOppgaveRequest) }
            .getOrElse {
                logger.error(it.message)
                throw it
            }

        val (url, response, responseBody) = httpPatch(body, "$patchEksisterendeOppgaveUrl/$oppgaveId")

        val harFeil = !response.isSuccessful
        if (harFeil) {
            logger.error("Feil ved endring av gosysoppgave. Url=[$url], HttpStatus=[${response.statusCode}], Response=$responseBody")
        }
        return HttpStatus.valueOf(response.statusCode) to if (harFeil) "Feil ved endring av gosysoppgave. Url=[$url], HttpStatus=[${response.statusCode}], Response=$responseBody" else null
    }

    private suspend fun httpPost(body: String, url: String): Triple<String, Response, String> {
        val (_, response, result) = "$oppgaveBaseUrl$url"
            .httpPost()
            .body(body)
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(scope).asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                NavHeaders.CallId to UUID.randomUUID().toString(),
                CorrelationIdHeader to coroutineContext.hentCorrelationId(),
                ConsumerIdHeaderKey to ConsumerIdHeaderValue
            ).awaitStringResponseResult()

        val responseBody = result.fold(
            { success -> success },
            { error ->
                when (error.response.body().isEmpty()) {
                    true -> "{}"
                    false -> String(error.response.body().toByteArray())
                }
            }
        )
        return Triple(url, response, responseBody)
    }

    private suspend fun httpPatch(body: String, url: String): Triple<String, Response, String> {
        val requestBuilder = "$oppgaveBaseUrl$url"
            .httpPatch()
            .body(body)
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.AUTHORIZATION to cachedAccessTokenClient.getAccessToken(scope).asAuthoriationHeader(),
                HttpHeaders.CONTENT_TYPE to "application/json",
                NavHeaders.CallId to UUID.randomUUID().toString(),
                CorrelationIdHeader to coroutineContext.hentCorrelationId(),
                ConsumerIdHeaderKey to ConsumerIdHeaderValue
            )

        logger.info("RequestBuilder: {}", requestBuilder.toString())

        val (_, response, result) = requestBuilder.awaitStringResponseResult()

        val responseBody = result.fold(
            { success -> success },
            { error ->
                when (error.response.body().isEmpty()) {
                    true -> "{}"
                    false -> String(error.response.body().toByteArray())
                }
            }
        )
        return Triple(url, response, responseBody)
    }
}
