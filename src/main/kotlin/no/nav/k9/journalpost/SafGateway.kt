package no.nav.k9.journalpost

import kotlinx.coroutines.reactive.awaitFirstOrNull
import no.nav.k9.AuthenticationClient
import no.nav.k9.JournalpostId
import no.nav.k9.hentAuthentication
import no.nav.k9.hentCorrelationId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import kotlin.coroutines.coroutineContext

@Service
internal class SafGateway(
        @Value("\${no.nav.saf.base_url}") safBaseUrl: URI,
        @Value("#{'\${no.nav.saf.scopes.hente_journalpost_scopes}'.split(',')}") private val henteJournalpostScopes: Set<String>,
        @Value("#{'\${no.nav.saf.scopes.hente_dokument_scopes}'.split(',')}") private val henteDokumentScopes: Set<String>,
        private val authenticationClient: AuthenticationClient
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SafGateway::class.java)

        private const val VariantType = "ARKIV"
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "Nav-Callid"
    }

    init {
        logger.info("SafBaseUr=$safBaseUrl")
    }

    private val client = WebClient.create(safBaseUrl.toString())

    internal suspend fun hentJournalpostInfo(journalpostId: JournalpostId) : Set<DokumentInfo> {
        val accessToken = authenticationClient
                .accessTokenClient
                .getAccessToken(
                        scopes = henteJournalpostScopes,
                        onBehalfOf = coroutineContext.hentAuthentication().accessToken
                )

        return emptySet()
    }

    internal suspend fun hentDokument(journalpostId: JournalpostId, dokumentId: DokumentId) : Dokument? {
        val accessToken = authenticationClient
                .accessTokenClient
                .getAccessToken(
                        scopes = henteDokumentScopes,
                        onBehalfOf = coroutineContext.hentAuthentication().accessToken
                )

        val response = client
                .get()
                .uri(journalpostId, dokumentId, VariantType)
                .header(ConsumerIdHeaderKey, ConsumerIdHeaderValue)
                .header(CorrelationIdHeader, coroutineContext.hentCorrelationId())
                .header(HttpHeaders.AUTHORIZATION, accessToken.asAuthoriationHeader())
                .retrieve()
                .toEntity(DataBuffer::class.java)
                .awaitFirstOrNull()

        return if (response == null) { null } else {
            Dokument(
                    contentType = response.headers.contentType ?: throw IllegalStateException("Content-Type ikke satt"),
                    dataBuffer = response.body?: throw java.lang.IllegalStateException("Body ikke satt")
            )
        }
    }
}

typealias DokumentId = String

data class Dokument(
        val contentType: MediaType,
        val dataBuffer: DataBuffer
)

data class DokumentInfo(
        val dokumentId: DokumentId
)

data class JournalpostInfo(
        val journalpostId: JournalpostId
)