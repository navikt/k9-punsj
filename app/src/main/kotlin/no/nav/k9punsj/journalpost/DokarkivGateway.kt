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
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
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

        ferdigstillJournalpost.kanFerdigstilles()
        val pair = "journalfoerendeEnhet" to enhetKode
        val body = emptyMap<String, String>().plus(pair)

        val fromValue = BodyInserters.fromValue(body)

        logger.info("Boddy$fromValue")
        val awaitFirst = client
            .patch()
            .uri { it.pathSegment("rest", "journalpostapi", "v1", "journalpost", journalpostId, "ferdigstill").build() }
            .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(fromValue)
            .retrieve()
            .toEntity(String::class.java)
            .awaitFirst()

        if (awaitFirst.statusCode.value() != 200) {
            logger.error("Feiler med" + awaitFirst.body)
        }

        return awaitFirst.statusCode.value()
    }

    internal suspend fun opprettJournalpost(journalpostRequest: JournalPostRequest): JournalPostResponse {
        val accessToken = cachedAccessTokenClient
            .getAccessToken(
                scopes = dokarkivScope,
                onBehalfOf = coroutineContext.hentAuthentication().accessToken
            )

        logger.info("Request body: {}", journalpostRequest)

        val response = client
            .post()
            .uri(URI.create(opprettJournalpostUrl))
            .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(journalpostRequest.dokarkivPayload())
            .retrieve()
            .onStatus(
                { status: HttpStatus -> status.isError },
                { errorResponse: ClientResponse ->
                    errorResponse.toEntity<String>().subscribe { entity: ResponseEntity<String> ->
                        logger.error("Feilet med å opprette journalpost. Feil: {}", entity.toString())
                    }
                    errorResponse.createException()
                }
            )
            .toEntity(JournalPostResponse::class.java)
            .awaitFirst()

        if (response.statusCode == HttpStatus.OK && response.body != null) {
            return response.body!!
        }

        throw IllegalStateException("Feilet med å opprette journalpost")
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
    private fun JournalpostId.oppdaterJournalpostUrl() = "$baseUrl/rest/journalpostapi/v1/journalpost/${this}"
    private val opprettJournalpostUrl = "$baseUrl/rest/journalpostapi/v1/journalpost"

    //    private fun JournalpostId.ferdigstillJournalpostUrl() = "rest/journalpostapi/v1/journalpost/${this}/ferdigstill"
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

data class JournalPostResponse(val journalpostId: String)

data class JournalPostRequest(
    internal val eksternReferanseId: String,
    internal val tittel: String,
    internal val brevkode: String,
    internal val tema: String,
    internal val kanal: Kanal,
    internal val journalposttype: JournalpostType,
    internal val fagsystem: FagsakSystem,
    internal val sakstype: SaksType,
    internal val saksnummer: String,
    internal val brukerIdent: String,
    internal val avsenderNavn: String,
    internal val tilleggsopplysninger: List<Tilleggsopplysning>,
    internal val pdf: ByteArray,
    internal val json: JSONObject
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
        return "JournalPostRequest(eksternReferanseId='$eksternReferanseId', tittel='$tittel', brevkode='$brevkode', tema='$tema', kanal=$kanal, journalposttype=$journalposttype, fagsystem=$fagsystem, sakstype=$sakstype, saksnummer='$saksnummer', brukerIdent='$brukerIdent', avsenderNavn='$avsenderNavn', tilleggsopplysninger=$tilleggsopplysninger, json=$json)"
    }


    private companion object {
        private fun ByteArray.base64() = Base64.getEncoder().encodeToString(this)
        private fun JSONObject.base64() = this.toString().toByteArray().base64()
    }
}

data class Tilleggsopplysning(
    val nokkel: String,
    val verdi: String
)

data class Sak(
    val fagsakId: String,
    val fagsakSystem: FagsakSystem = FagsakSystem.K9,
    val sakstype: SaksType
)

data class DokarkivDokument(
    val tittel: String,
    val brevkode: String? = null, // Eller brevkode + dokumentkategori
    val dokumentVarianter: List<DokumentVariant>
)

data class DokumentVariant(
    val filtype: FilType,
    val variantformat: VariantFormat,
    val fysiskDokument: ByteArray
)

data class AvsenderMottaker(val id: String, val idType: IdType, val navn: String? = null)
data class Bruker(val id: String, val idType: IdType)

enum class IdType { FNR }
enum class FilType { PDFA, JSON }
enum class VariantFormat { ORIGINAL, ARKIV }
enum class Tema { OMS }
enum class JournalpostType { NOTAT }
enum class FagsakSystem { K9 }
enum class SaksType { FAGSAK, GENERELL_SAK, ARKIVSAK }
enum class Kanal { NAV_NO, ALTINN, EESSI, INGEN_DISTRIBUSJON }
