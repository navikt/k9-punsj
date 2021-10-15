package no.nav.k9punsj.journalpost

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.helsesjekk
import no.nav.k9punsj.hentAuthentication
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.rest.web.JournalpostId
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
        @Qualifier("azure")private val accessTokenClient: AccessTokenClient
) : ReactiveHealthIndicator {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SafGateway::class.java)
        private const val VariantType = "ARKIV"
        private const val ConsumerIdHeaderKey = "Nav-Consumer-Id"
        private const val ConsumerIdHeaderValue = "k9-punsj"
        private const val CorrelationIdHeader = "Nav-Callid"
        private const val MaxDokumentSize = 16 * 1024 * 1024
        private val IkkeStøttedeStatuser = setOf("UTGAAR","AVBRUTT","FEILREGISTRERT")
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

        if (safResponse.errors != null) {
            logger.warn("SafErrors=${safResponse.errors}")
        }

        // For journalposter som kommer digitalt er det kun ettersendelser som skal kunne punsjes.
        if (journalpost?.erIkkeStøttetDigitalJournalpost == true) throw IkkeStøttetJournalpost().also {
            logger.warn("Ikke støttet digital journalpost. K9Kilde=[${journalpost.k9Kilde}], K9Type=[${journalpost.k9Type}]")
        }

        if (safResponse.journalpostFinnesIkke) return null
        // For journalposter i spesielle statuser. Krever spesialrettighet for å håndtere disse statusene i arkivet.
        // Disse statusene støttes uansett ikke av Punsj så gir samme feilmelding som om man har tilgang til disse statusene.
        if (safResponse.manglerTilgangPåGrunnAvStatus) throw IkkeStøttetJournalpost().also {
            logger.warn("Saksbehandler mangler tilgang på grunn av journalstatus.")
        }
        if (safResponse.manglerTilgang) throw IkkeTilgang()

        check(errors == null) {"Feil ved oppslag mot SAF graphql. SafErrors=$errors"}

        // For saksbehandlere som har tilgang til å åpne journalposter i spesielle statuser.
        // Disse statusene støttes uansett ikke av Punsj så gir samme feilmelding som om man ikke har tilgang.
        if (IkkeStøttedeStatuser.contains(journalpost?.journalstatus)) throw IkkeStøttetJournalpost().also {
            logger.warn("Ikke støttet journalstatus ${journalpost?.journalstatus}.")
        }

        // Kan ikke oppdatere eller ferdigstille Notater som er under redigering.
        if(journalpost?.journalposttype == "N" &&
            journalpost?.journalstatus?.equals("UNDER_ARBEID") == true) throw IkkeStøttetJournalpost().also {
            logger.warn("Ikke støttet journalpost: Type NOTAT med status UNDER_ARBEID")
        }

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
