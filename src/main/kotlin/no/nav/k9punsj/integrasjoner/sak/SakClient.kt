package no.nav.k9punsj.integrasjoner.sak

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.utils.WebClienttUtils.håndterFeil
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

@Service
class SakClient(
    @Value("\${no.nav.sak.base_url}") private val baseUrl: URI,
    @Qualifier("sts") private val accessTokenClient: AccessTokenClient,
) {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val client = WebClient
        .builder()
        .baseUrl("$baseUrl/api/v1/saker")
        .build()

    internal suspend fun forsikreSakskoblingFinnes(
        saksnummer: String,
        søker: String,
        correlationId: String
    ) {

        @Language("JSON")
        val dto = """
            {
              "tema": "OMS",
              "applikasjon": "K9",
              "aktoerId": "$søker",
              "fagsakNr": "$saksnummer"
            }
        """.trimIndent()

        val responseEntity = kotlin.runCatching { client
            .post()
            .header(HttpHeaders.AUTHORIZATION, cachedAccessTokenClient.getAccessToken(emptySet()).asAuthoriationHeader())
            .header("X-Correlation-ID", correlationId)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(dto))
            .retrieve()
            .toEntity(String::class.java)
            .awaitFirst()
        }.håndterFeil()

        when (responseEntity.statusCode) {
            HttpStatus.CREATED -> logger.info("Opprettet sakskobling.")
            HttpStatus.CONFLICT -> logger.info("Sakskobling finnes allerede.")
            else -> throw IllegalStateException(
                "Feil fra Sak. HttpStatusCode=[${responseEntity.statusCode}], Response=[${responseEntity.body}]"
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(SakClient::class.java)
    }
}