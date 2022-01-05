package no.nav.k9punsj.journalpost

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.onError
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.hentAuthentication
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.journalpost.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import no.nav.k9punsj.journalpost.JoarkTyper.JournalpostType.Companion.somJournalpostType
import no.nav.k9punsj.journalpost.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.rest.web.JournalpostId
import org.json.JSONObject
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
import kotlin.coroutines.coroutineContext

@Service
class DokarkivGateway(
    @Value("\${no.nav.dokarkiv.base_url}") private val baseUrl: URI,
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient,
    @Value("#{'\${no.nav.dokarkiv.scope}'.split(',')}") private val dokarkivScope: Set<String>,
) {

    internal suspend fun oppdaterJournalpostData(
        dataFraSaf: JSONObject?,
        journalpostId: JournalpostId,
        identitetsnummer: Identitetsnummer,
        enhetKode: String,
    ): Int {
        val ferdigstillJournalpost =
            dataFraSaf?.mapFerdigstillJournalpost(journalpostId.somJournalpostId(), identitetsnummer)
        val oppdatertPayload = ferdigstillJournalpost!!.oppdaterPayloadGenerellSak()

        val accessToken = cachedAccessTokenClient
            .getAccessToken(
                scopes = dokarkivScope,
                onBehalfOf = coroutineContext.hentAuthentication().accessToken
            )

        val (request, response, result) = journalpostId.oppdaterJournalpostUrl()
            .httpPut()
            .jsonBody(JSONObject(oppdatertPayload).toString())
            .header(
                HttpHeaders.ACCEPT to "application/json",
                ConsumerIdHeaderKey to ConsumerIdHeaderValue,
                CorrelationIdHeader to coroutineContext.hentCorrelationId(),
                HttpHeaders.AUTHORIZATION to accessToken.asAuthoriationHeader()
            ).awaitStringResponseResult()

        result.onError {
            håndterFeil(it, request, response)
        }

        val ferdigstillPayload = ferdigstillJournalpost.ferdigstillPayload(enhetKode = enhetKode)

        val awaitFirst = client
            .patch()
            .uri { it.pathSegment(journalpostId.ferdigstillJournalpostUrl()).build() }
            .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(JSONObject(ferdigstillPayload).toString())
            .retrieve()
            .toEntity(String::class.java)
            .awaitFirst()

        if (awaitFirst.statusCode.value() != 200) {
            logger.error("Feiler med" + awaitFirst.body)
        }

        return awaitFirst.statusCode.value()
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SafGateway::class.java)
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "Nav-Callid"
        private const val MaxDokumentSize = 16 * 1024 * 1024
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

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private fun JournalpostId.oppdaterJournalpostUrl() = "$baseUrl/rest/journalpostapi/v1/journalpost/${this}"
    private fun JournalpostId.ferdigstillJournalpostUrl() = "/rest/journalpostapi/v1/journalpost/${this}/ferdigstill"
    private fun JSONObject.stringOrNull(key: String) = when (notNullNotBlankString(key)) {
        true -> getString(key)
        false -> null
    }

    private fun JSONObject.notNullNotBlankString(key: String) =
        has(key) && get(key) is String && getString(key).isNotBlank()

    private fun håndterFeil(
        it: FuelError,
        request: Request,
        response: Response,
    ) {
        val feil = it.response.body().asString("text/plain")
        logger.error(
            "Error response = '$feil' fra '${request.url}'"
        )
        logger.error(it.toString())
        when (response.statusCode) {
            400 -> throw FeilIAksjonslogg(feil)
            401 -> throw UgyldigToken(feil)
            403 -> throw IkkeTilgang(feil)
            404 -> throw IkkeFunnet(feil)
            500 -> throw InternalServerErrorDoarkiv(feil)
            else -> {
                throw IllegalStateException("${response.statusCode} -> " + feil)
            }
        }
    }

    private fun JSONObject.mapFerdigstillJournalpost(
        journalpostId: no.nav.k9punsj.journalpost.JournalpostId,
        identitetsnummer: Identitetsnummer,
    ) =
        getJSONObject("journalpost")
            .let { journalpost ->
                FerdigstillJournalpost(
                    journalpostId = journalpostId,
                    avsendernavn = journalpost.getJSONObject("avsenderMottaker").stringOrNull("navn"),
                    status = journalpost.getString("journalstatus").somJournalpostStatus(),
                    type = journalpost.getString("journalposttype").somJournalpostType(),
                    tittel = journalpost.stringOrNull("tittel"),
                    dokumenter = journalpost.getJSONArray("dokumenter").map { it as JSONObject }.map {
                        FerdigstillJournalpost.Dokument(
                            dokumentId = it.getString("dokumentInfoId"),
                            tittel = it.stringOrNull("tittel")
                        )
                    }.toSet(),
                    bruker = FerdigstillJournalpost.Bruker(identitetsnummer)
                )
            }

}
