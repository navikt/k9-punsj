package no.nav.k9punsj.tilgangskontroll.azuregraph

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.idToken
import no.nav.k9punsj.utils.Cache
import no.nav.k9punsj.utils.CacheObject
import no.nav.k9punsj.utils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import java.time.LocalDateTime
import kotlin.coroutines.coroutineContext

@Configuration
@StandardProfil
class AzureGraphService(
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient,
    @Value("\${MS_GRAPH_URL:https://graph.microsoft.com/v1.0}") private val msGraphUrl: String
) : IAzureGraphService {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val cache = Cache<String>()
    val log = LoggerFactory.getLogger("AzureGraphService")!!

    override suspend fun hentIdentTilInnloggetBruker(): String {
        return coroutineContext.idToken().getNavIdent()
    }

    override suspend fun hentEnhetForInnloggetBruker(): String {
        val idToken = coroutineContext.idToken()
        val username = idToken.getNavIdent() + "_office_location"
        val cachedObject = cache.get(username)

        if (cachedObject == null) {
            val enhetAccessToken =
                cachedAccessTokenClient.getAccessToken(
                    scopes = setOf("https://graph.microsoft.com/user.read"),
                    onBehalfOf = idToken.value
                )

            val (request, _, result) = "$msGraphUrl/me?\$select=officeLocation"
                .httpGet()
                .header(
                    HttpHeaders.ACCEPT to "application/json",
                    HttpHeaders.AUTHORIZATION to "Bearer ${enhetAccessToken.token}"
                ).awaitStringResponseResult()

            val json = result.fold(
                { success -> success },
                { error ->
                    log.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    log.error(error.toString())
                    throw IllegalStateException("Feil ved henting av saksbehandlers enhet")
                }
            )

            return try {
                val officeLocationData = objectMapper().readValue<OfficeLocation>(json)
                val enhet = when {
                    officeLocationData.officeLocation != null -> {
                        log.info("Bruker officeLocation for enhet")
                        officeLocationData.officeLocation
                    }
                    officeLocationData.streetAddress != null -> {
                        log.info("Bruker streetAddress for enhet")
                        officeLocationData.streetAddress
                    }
                    else -> {
                        log.warn("Ingen officeLocation eller streetAddress funnet")
                        ""
                    }
                }
                cache.set(username, CacheObject(enhet, LocalDateTime.now().plusDays(180)))
                return enhet
            } catch (e: Exception) {
                log.error(
                    "Feilet deserialisering",
                    e
                )
                ""
            }
        } else {
            return cachedObject.value
        }
    }
}
