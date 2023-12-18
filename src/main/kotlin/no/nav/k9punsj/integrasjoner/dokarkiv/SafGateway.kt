package no.nav.k9punsj.integrasjoner.dokarkiv

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.felles.FeilIAksjonslogg
import no.nav.k9punsj.felles.IkkeFunnet
import no.nav.k9punsj.felles.IkkeStøttetJournalpost
import no.nav.k9punsj.felles.IkkeTilgang
import no.nav.k9punsj.felles.JournalpostId
import no.nav.k9punsj.felles.NotatUnderArbeidFeil
import no.nav.k9punsj.felles.UgyldigToken
import no.nav.k9punsj.felles.UventetFeil
import no.nav.k9punsj.hentAuthentication
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.integrasjoner.dokarkiv.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import no.nav.k9punsj.integrasjoner.dokarkiv.JoarkTyper.JournalpostType.Companion.somJournalpostType
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.tilgangskontroll.helsesjekk
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*
import kotlin.coroutines.coroutineContext

@Service
class SafGateway(
    @Value("\${no.nav.saf.base_url}") private val baseUrl: URI,
    @Value("#{'\${no.nav.saf.scopes.hente_journalpost_scopes}'.split(',')}") private val henteJournalpostScopes: Set<String>,
    @Value("#{'\${no.nav.saf.scopes.hente_dokument_scopes}'.split(',')}") private val henteDokumentScopes: Set<String>,
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SafGateway::class.java)
        private const val VariantType = "ARKIV"
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "Nav-Callid"
        private const val MaxDokumentSize = 16 * 1024 * 1024
        private val IkkeStøttedeStatuser = setOf("UTGAAR", "AVBRUTT", "FEILREGISTRERT")
    }

    init {
        logger.info("SafBaseUr=$baseUrl")
        logger.info("HenteJournalpostScopes=${henteJournalpostScopes.joinToString()}")
        logger.info("HenteDokumentScopes=${henteDokumentScopes.joinToString()}")
    }

    private val GraphQlUrl: String = "$baseUrl/graphql"

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

    internal suspend fun hentJournalpost(journalpostId: String): SafDtos.Journalpost? {
        val accessToken = cachedAccessTokenClient.getAccessToken(
            scopes = henteJournalpostScopes,
            onBehalfOf = coroutineContext.hentAuthentication().accessToken
        )

        val correlationId = try {
            coroutineContext.hentCorrelationId()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }

        val response = try {
            client
                .post()
                .uri { it.pathSegment("graphql").build() }
                .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                .header(CorrelationIdHeader, correlationId)
                .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(SafDtos.JournalpostQuery(journalpostId))
                .retrieve()
                .toEntity(SafDtos.JournalpostResponseWrapper::class.java)
                .awaitFirst()
        } catch (e: Exception) {
            throw IllegalStateException("Feil ved oppslag mot SAF graphql. ${e.message}", e)
        }

        val safResponse = response.body
        val errors = safResponse?.errors
        val journalpost = response.body?.data?.journalpost

        check(safResponse != null) { "Ingen response entity fra SAF" }

        if (safResponse.errors != null) {
            logger.warn("SafErrors=${safResponse.errors}")
        }

        if (safResponse.journalpostFinnesIkke) return null
        // For journalposter i spesielle statuser. Krever spesialrettighet for å håndtere disse statusene i arkivet.
        // Disse statusene støttes uansett ikke av Punsj så gir samme feilmelding som om man har tilgang til disse statusene.
        if (safResponse.manglerTilgangPåGrunnAvStatus) throw IkkeStøttetJournalpost().also {
            logger.warn("Saksbehandler mangler tilgang på grunn av journalstatus.")
        }
        if (safResponse.manglerTilgang) throw IkkeTilgang("Saksbehandler har ikke tilgang.")

        check(errors == null) { "Feil ved oppslag mot SAF graphql. SafErrors=$errors" }

        return journalpost
    }

    internal suspend fun hentJournalposter(journalpostIder: List<String>): List<SafDtos.Journalpost?> =
        journalpostIder.map { hentJournalpost(it) }.toList()

    internal suspend fun hentJournalpostInfo(
        journalpostId: String
    ): SafDtos.Journalpost? {
        val journalpost = hentJournalpost(journalpostId)

        // For journalposter som kommer digitalt er det kun ettersendelser som skal kunne punsjes.
        if (journalpost?.erIkkeStøttetDigitalJournalpost == true) throw IkkeStøttetJournalpost().also {
            logger.warn("Ikke støttet digital journalpost. K9Kilde=[${journalpost.k9Kilde}], K9Type=[${journalpost.k9Type}]")
        }

        // For saksbehandlere som har tilgang til å åpne journalposter i spesielle statuser.
        // Disse statusene støttes uansett ikke av Punsj så gir samme feilmelding som om man ikke har tilgang.
        if (IkkeStøttedeStatuser.contains(journalpost?.journalstatus)) throw IkkeStøttetJournalpost().also {
            logger.warn("Ikke støttet journalstatus ${journalpost?.journalstatus}.")
        }

        // Kan ikke oppdatere eller ferdigstille Notater som er under redigering.
        if (journalpost?.journalposttype == "N" &&
            journalpost.journalstatus?.equals("UNDER_ARBEID") == true
        ) throw NotatUnderArbeidFeil().also {
            logger.warn("Ikke støttet journalpost: Type NOTAT med status UNDER_ARBEID")
        }

        return journalpost
    }

    internal suspend fun hentDataFraSaf(journalpostId: String): JSONObject {
        val accessToken = cachedAccessTokenClient.getAccessToken(
            scopes = henteJournalpostScopes,
            onBehalfOf = coroutineContext.hentAuthentication().accessToken
        )

        val body = objectMapper().writeValueAsString(SafDtos.FerdigstillJournalpostQuery(journalpostId))
        val (request, response, result) = GraphQlUrl
            .httpPost()
            .body(body)
            .header(
                HttpHeaders.ACCEPT to "application/json",
                HttpHeaders.CONTENT_TYPE to "application/json",
                ConsumerIdHeaderKey to ConsumerIdHeaderValue,
                CorrelationIdHeader to coroutineContext.hentCorrelationId(),
                HttpHeaders.AUTHORIZATION to accessToken.asAuthoriationHeader()
            ).awaitStringResponseResult()

        return result.fold(
            success = {
                it.safData()
            },
            failure = {
                throw håndterFeil(it, request, response)
            }
        )
    }

    internal suspend fun hentFerdigstillJournalpost(
        journalpostId: JournalpostId
    ): FerdigstillJournalpost {
        return hentDataFraSaf(journalpostId.toString())
            .mapFerdigstillJournalpost(journalpostId)
    }

    internal suspend fun hentDokument(journalpostId: String, dokumentId: String): Dokument? {
        val accessToken = cachedAccessTokenClient
            .getAccessToken(
                scopes = henteDokumentScopes,
                onBehalfOf = coroutineContext.hentAuthentication().accessToken
            )

        val (statusCode, entity) = client
            .get()
            .uri { it.pathSegment("rest", "hentdokument", journalpostId, dokumentId, VariantType).build() }
            .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
            .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
            .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
            .awaitExchange { Pair(it.statusCode(), it.awaitEntity<DataBuffer>()) }

        return when (statusCode.value()) {
            200 -> {
                Dokument(
                    contentType = entity.headers.contentType ?: throw IllegalStateException("Content-Type ikke satt"),
                    dataBuffer = entity.body ?: throw IllegalStateException("Body ikke satt")
                )
            }
            404 -> null
            403 -> throw IkkeTilgang("Saksbehandler har ikke tilgang.")
            else -> {
                throw IllegalStateException("Feil ved henting av dokument fra SAF. $statusCode")
            }
        }
    }

    internal fun håndterFeil(
        it: FuelError,
        request: Request,
        response: Response,
    ): Throwable {
        val feil = it.response.body().asString("text/plain")
        logger.error(
            "Error response = '$feil' fra '${request.url}'"
        )
        logger.error(it.toString())
        return when (response.statusCode) {
            400 -> FeilIAksjonslogg(feil)
            401 -> UgyldigToken(feil)
            403 -> IkkeTilgang(feil)
            404 -> IkkeFunnet()
            500 -> UventetFeil(feil)
            else -> IllegalStateException("${response.statusCode} -> " + feil)
        }
    }

    internal fun String.safData() = JSONObject(this).getJSONObject("data")

    private fun JSONObject.notNullNotBlankString(key: String) =
        has(key) && get(key) is String && getString(key).isNotBlank()

    private fun JSONObject.stringOrNull(key: String) = when (notNullNotBlankString(key)) {
        true -> getString(key)
        false -> null
    }

    internal fun JSONObject.mapFerdigstillJournalpost(journalpostId: JournalpostId) =
        getJSONObject("journalpost")
            .let { journalpost ->
                FerdigstillJournalpost(
                    journalpostId = journalpostId,
                    avsenderIdType = journalpost.getJSONObject("avsenderMottaker").stringOrNull("idType"),
                    status = journalpost.getString("journalstatus").somJournalpostStatus(),
                    type = journalpost.getString("journalposttype").somJournalpostType(),
                    tittel = journalpost.stringOrNull("tittel"),
                    dokumenter = journalpost.getJSONArray("dokumenter").map { it as JSONObject }.map {
                        FerdigstillJournalpost.Dokument(
                            dokumentId = it.getString("dokumentInfoId"),
                            tittel = it.stringOrNull("tittel")
                        )
                    }.toSet(),
                    sak = FerdigstillJournalpost.Sak(
                        sakstype = null,
                        fagsaksystem = null,
                        fagsakId = null
                    )
                )
            }

    override fun health() = Mono.just(
        accessTokenClient.helsesjekk(
            operasjon = "hente-journalpost",
            scopes = henteJournalpostScopes,
            initialHealth = accessTokenClient.helsesjekk(
                operasjon = "hente-dokument",
                scopes = henteDokumentScopes
            )
        )
    )
}

data class Dokument(
    val contentType: MediaType,
    val dataBuffer: DataBuffer
)
