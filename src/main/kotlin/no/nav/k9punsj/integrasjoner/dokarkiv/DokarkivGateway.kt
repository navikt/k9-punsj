package no.nav.k9punsj.integrasjoner.dokarkiv

import com.fasterxml.jackson.databind.node.ObjectNode
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
import no.nav.k9punsj.felles.FeilIAksjonslogg
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.IkkeFunnet
import no.nav.k9punsj.felles.IkkeTilgang
import no.nav.k9punsj.felles.JournalpostId
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.felles.Sak
import no.nav.k9punsj.felles.UgyldigToken
import no.nav.k9punsj.felles.UventetFeil
import no.nav.k9punsj.hentAuthentication
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.integrasjoner.dokarkiv.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import no.nav.k9punsj.integrasjoner.dokarkiv.JoarkTyper.JournalpostType.Companion.somJournalpostType
import no.nav.k9punsj.utils.WebClienttUtils.håndterFeil
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import java.net.URI
import java.util.*
import kotlin.coroutines.coroutineContext

@Service
class DokarkivGateway(
    @Value("\${no.nav.dokarkiv.base_url}") private val baseUrl: URI,
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient,
    @Value("#{'\${no.nav.dokarkiv.scope}'.split(',')}") private val dokarkivScope: Set<String>
) {

    internal suspend fun oppdaterJournalpostDataOgFerdigstill(
        dataFraSaf: JSONObject,
        journalpostId: String,
        identitetsnummer: Identitetsnummer,
        enhetKode: String,
        sak: Sak
    ): Pair<HttpStatusCode, String> {
        val ferdigstillJournalpost =
            dataFraSaf.mapFerdigstillJournalpost(
                journalpostId = journalpostId.somJournalpostId(),
                identitetsnummer = identitetsnummer,
                sak = FerdigstillJournalpost.Sak(
                    sakstype = sak.sakstype.name,
                    fagsakId = sak.fagsakId,
                    fagsaksystem = sak.fagsaksystem?.name
                )
            )

        val oppdatertPayload = ferdigstillJournalpost.oppdaterPayloadMedSak()

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

        ferdigstillJournalpost.kanFerdigstilles()
        val responseFerdigstiltJournalpost = ferdigstillJournalpost(journalpostId, enhetKode)

        if (responseFerdigstiltJournalpost.statusCode.value() != 200) {
            logger.error("Feiler med" + responseFerdigstiltJournalpost.body)
        }

        return responseFerdigstiltJournalpost.statusCode to (responseFerdigstiltJournalpost.body
            ?: "Feilet med å ferdigstille journalpost")
    }

    internal suspend fun opprettOgFerdigstillJournalpost(journalpostRequest: JournalPostRequest): JournalPostResponse {
        val accessToken = cachedAccessTokenClient
            .getAccessToken(
                scopes = dokarkivScope,
                onBehalfOf = coroutineContext.hentAuthentication().accessToken
            )

        val body = BodyInserters.fromValue(journalpostRequest.dokarkivPayload())

        val response = client
            .post()
            .uri(URI.create(opprettOgFerdigstillJournalpostUrl))
            .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(JournalPostResponse::class.java)
            .awaitFirst()

        if (response.statusCode == HttpStatus.CREATED && response.body != null) {
            return response.body!!
        }

        throw IllegalStateException("Feilet med å opprette journalpost: ${response.statusCode} -> ${response.body}")
    }

    internal suspend fun ferdigstillJournalpost(journalpostId: String, enhet: String): ResponseEntity<String> {
        val body = BodyInserters.fromValue("""{"journalfoerendeEnhet": "$enhet"}""".trimIndent())

        val accessToken = cachedAccessTokenClient
            .getAccessToken(scopes = dokarkivScope, onBehalfOf = coroutineContext.hentAuthentication().accessToken)

        return kotlin.runCatching {
            client
                .patch()
                .uri(URI(journalpostId.ferdigstillJournalpostUrl()))
                .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String::class.java)
                .awaitFirst()
        }.håndterFeil()
    }

    internal suspend fun oppdaterJournalpostForFerdigstilling(
        ferdigstillJournalpost: FerdigstillJournalpost
    ) {
        val url = ferdigstillJournalpost.journalpostId.toString().oppdaterJournalpostUrl()
        val accessToken = cachedAccessTokenClient
            .getAccessToken(
                scopes = dokarkivScope,
                onBehalfOf = coroutineContext.hentAuthentication().accessToken
            )

        val body = BodyInserters.fromValue(ferdigstillJournalpost.oppdaterPayloadMedSak())

        val response = client
            .put()
            .uri(URI.create(url))
            .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(String::class.java)
            .awaitFirst()

        check(response.statusCode.is2xxSuccessful) {
            "Feil ved oppdatering av journalpost. HttpStatus=[${response.statusCode.value()}, Response=[${response.body}], Url=[$url]"
        }
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(DokarkivGateway::class.java)
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
    private fun String.oppdaterJournalpostUrl() = "$baseUrl/rest/journalpostapi/v1/journalpost/$this"
    private val opprettOgFerdigstillJournalpostUrl = "$baseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
    private fun String.ferdigstillJournalpostUrl() = "$baseUrl/rest/journalpostapi/v1/journalpost/$this/ferdigstill"

    private fun JSONObject.stringOrNull(key: String) = when (notNullNotBlankString(key)) {
        true -> getString(key)
        false -> null
    }

    private fun JSONObject.notNullNotBlankString(key: String) =
        has(key) && get(key) is String && getString(key).isNotBlank()

    private fun håndterFeil(
        it: FuelError,
        request: Request,
        response: Response
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
            404 -> throw IkkeFunnet()
            500 -> throw UventetFeil(feil)
            else -> {
                throw IllegalStateException("${response.statusCode} -> " + feil)
            }
        }
    }

    private fun JSONObject.mapFerdigstillJournalpost(
        journalpostId: JournalpostId,
        identitetsnummer: Identitetsnummer,
        sak: FerdigstillJournalpost.Sak
    ) =
        getJSONObject("journalpost")
            .let { journalpost ->
                val avsenderMottaker = journalpost.getJSONObject("avsenderMottaker")
                val avsenderIdType = when (avsenderMottaker.stringOrNull("idType")) {
                    null, "NULL" -> null
                    else -> avsenderMottaker.getString("idType")
                }

                val journalpostStatus = journalpost.getString("journalstatus").somJournalpostStatus()
                val journalpostType = journalpost.getString("journalposttype").somJournalpostType()
                val tittel = journalpost.stringOrNull("tittel")
                val dokumenter = journalpost.getJSONArray("dokumenter").map { it as JSONObject }.map {
                    FerdigstillJournalpost.Dokument(
                        dokumentId = it.getString("dokumentInfoId"),
                        tittel = it.stringOrNull("tittel")
                    )
                }.toSet()
                val bruker = FerdigstillJournalpost.Bruker(identitetsnummer)

                FerdigstillJournalpost(
                    journalpostId = journalpostId,
                    avsenderIdType = avsenderIdType,
                    status = journalpostStatus,
                    type = journalpostType,
                    tittel = tittel,
                    dokumenter = dokumenter,
                    bruker = bruker,
                    sak = sak
                )
            }
}

data class JournalPostResponse(val journalpostId: String)

data class JournalPostRequest(
    internal val eksternReferanseId: String,
    internal val tittel: String,
    internal val brevkode: String,
    internal val tema: Tema,
    internal val kanal: Kanal,
    internal val journalposttype: JournalpostType,
    internal val dokumentkategori: DokumentKategori,
    internal val fagsystem: FagsakSystem,
    internal val sakstype: SaksType,
    internal val saksnummer: String,
    internal val brukerIdent: String,
    internal val avsenderNavn: String,
    internal val pdf: ByteArray,
    internal val json: ObjectNode
) {
    internal fun dokarkivPayload(): String {
        @Language("JSON")
        val json = """
            {
              "eksternReferanseId": "$eksternReferanseId",
              "tittel": "$tittel",
              "avsenderMottaker": {
                "navn": "$avsenderNavn"
              },
              "bruker": {
                "id": "$brukerIdent",
                "idType": "FNR"
              },
              "sak": {
                "sakstype": "$sakstype",
                "fagsakId": "$saksnummer",
                "fagsaksystem": "${fagsystem.name}"
              },
              "dokumenter": [{
                "tittel": "$tittel",
                "brevkode": "$brevkode",
                "dokumentkategori": "$dokumentkategori",
                "dokumentVarianter": [{
                  "filtype": "PDFA",
                  "variantformat": "ARKIV",
                  "fysiskDokument": "${pdf.base64()}"
                },{
                  "filtype": "JSON",
                  "variantformat": "ORIGINAL",
                  "fysiskDokument": "${json.base64()}"
                }]
              }],
              "tema": "$tema",
              "journalposttype": "$journalposttype",
              "kanal": "$kanal",
              "journalfoerendeEnhet": "9999"
            }
        """.trimIndent()
        return json
    }

    override fun toString(): String {
        return "JournalPostRequest(eksternReferanseId='$eksternReferanseId', tittel='$tittel', brevkode='$brevkode', tema='$tema', kanal=$kanal, journalposttype=$journalposttype, fagsystem=$fagsystem, sakstype=$sakstype, saksnummer='$saksnummer', brukerIdent='***', avsenderNavn='***', json=$json)"
    }

    private companion object {
        private fun ByteArray.base64() = Base64.getEncoder().encodeToString(this)
        private fun ObjectNode.base64() = this.toString().toByteArray().base64()
    }
}

enum class Tema { OMS }
enum class JournalpostType { NOTAT }
enum class DokumentKategori { IS }
enum class FagsakSystem { K9 }
enum class SaksType { FAGSAK, GENERELL_SAK, ARKIVSAK }
enum class Kanal { NAV_NO, ALTINN, EESSI, INGEN_DISTRIBUSJON }
