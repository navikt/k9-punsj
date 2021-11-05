package no.nav.k9punsj.azuregraph

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.hentAuthentication
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.info.ITokenService
import no.nav.k9punsj.utils.Cache
import no.nav.k9punsj.utils.CacheObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import java.time.LocalDateTime
import kotlin.coroutines.coroutineContext

@Configuration
@StandardProfil
class AzureGraphService(
    @Qualifier("azure") private val accessTokenClient: AccessTokenClient,
    private val tokenService: ITokenService,
) : IAzureGraphService {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val cache = Cache<String>()
    val log = LoggerFactory.getLogger("AzureGraphService")!!

    override suspend fun hentIdentTilInnloggetBruker(): String {
        val accessToken = coroutineContext.hentAuthentication().accessToken
        val iIdToken = tokenService.decodeToken(accessToken)
        val username = iIdToken.getUsername()
        val cachedObject = cache.get(username)
        if (cachedObject == null) {
            val graphAccessToken =
                cachedAccessTokenClient.getAccessToken(
                    scopes = setOf("https://graph.microsoft.com/.default"),
                    onBehalfOf = accessToken
                )

            val (request, _, result) = "https://graph.microsoft.com/v1.0/me?\$select=onPremisesSamAccountName"
                .httpGet()
                .header(
                    HttpHeaders.ACCEPT to "application/json",
                    HttpHeaders.AUTHORIZATION to "Bearer ${graphAccessToken.token}"
                ).awaitStringResponseResult()

            val json = result.fold(
                { success -> success },
                { error ->
                    log.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    log.error(error.toString())
                    throw IllegalStateException("Feil ved henting av saksbehandlers id")
                }
            )

            return try {
                val ident = objectMapper().readValue<AccountName>(json).onPremisesSamAccountName
                cache.set(username, CacheObject(ident, LocalDateTime.now().plusDays(180)))
                ident
            } catch (e: Exception) {
                log.error(
                    "Feilet deserialisering", e
                )
                ""
            }

        } else {
            return cachedObject.value
        }
    }

    override suspend fun hentEnhetForInnloggetBruker(): String {
        TODO("Not yet implemented")
    }
}



