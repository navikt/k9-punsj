package no.nav.k9.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.JournalpostId
import no.nav.k9.helsesjekk
import no.nav.k9.hentAuthentication
import no.nav.k9.hentCorrelationId
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
import kotlin.coroutines.coroutineContext

@Service
class SafGateway(
        @Value("\${no.nav.saf.base_url}") safBaseUrl: URI,
        @Value("#{'\${no.nav.saf.scopes.hente_journalpost_scopes}'.split(',')}") private val henteJournalpostScopes: Set<String>,
        @Value("#{'\${no.nav.saf.scopes.hente_dokument_scopes}'.split(',')}") private val henteDokumentScopes: Set<String>,
        @Qualifier("saf") private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SafGateway::class.java)
        private const val VariantType = "ARKIV"
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "Nav-Callid"
        private const val MaxDokumentSize = 5 * 1024 * 1024
    }

    init {
        logger.info("SafBaseUr=$safBaseUrl")
        logger.info("HenteJournalpostScopes=${henteJournalpostScopes.joinToString()}")
        logger.info("HenteDokumentScopes=${henteDokumentScopes.joinToString()}")
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

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    internal suspend fun hentJournalpostInfo(journalpostId: JournalpostId): SafDtos.Journalpost? {
        val accessToken = cachedAccessTokenClient
                .getAccessToken(
                        scopes = henteJournalpostScopes,
                        onBehalfOf = coroutineContext.hentAuthentication().accessToken
                )
        val response = client
                .post()
                .uri { it.pathSegment("graphql").build() }
                .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(SafDtos.JournalpostQuery(journalpostId))
                .retrieve()
                .toEntity(SafDtos.JournalpostResponseWrapper::class.java)
                .awaitFirst()

        val safResponse = response.body
        val errors = safResponse?.errors
        val journalpost = response.body?.data?.journalpost

        check(safResponse != null) {"Ingen response entity fra SAF"}

        if (safResponse.journalpostFinnesIkke) return null
        if (safResponse.manglerTilgang) throw IkkeTilgang()

        check(errors == null) {"Feil ved oppslag mot SAF graphql. Error = $errors"}

        return journalpost
    }

    internal suspend fun hentDokument(journalpostId: JournalpostId, dokumentId: DokumentId): Dokument? {
        val accessToken = cachedAccessTokenClient
                .getAccessToken(
                        scopes = henteDokumentScopes,
                        onBehalfOf = coroutineContext.hentAuthentication().accessToken
                )

        val clientResponse = client
                .get()
                .uri { it.pathSegment("rest", "hentdokument", journalpostId, dokumentId, VariantType).build() }
                .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
                .awaitExchange()


        return when (clientResponse.rawStatusCode()) {
            200 -> {
                val entity = clientResponse.awaitEntity<DataBuffer>()
                Dokument(
                        contentType = entity.headers.contentType ?: throw IllegalStateException("Content-Type ikke satt"),
                        dataBuffer = entity.body ?: throw IllegalStateException("Body ikke satt")
                )
            }
            404 -> null
            403 -> throw IkkeTilgang()
            else -> {
                throw IllegalStateException("Feil ved henting av dokument fra SAF. ${clientResponse.statusCode()}")
            }
        }
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


typealias DokumentId = String

data class Dokument(
        val contentType: MediaType,
        val dataBuffer: DataBuffer
)