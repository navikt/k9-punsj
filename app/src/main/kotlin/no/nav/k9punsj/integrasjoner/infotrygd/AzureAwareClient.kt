package no.nav.k9punsj.integrasjoner.infotrygd

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpGet
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.springframework.http.HttpHeaders
import java.net.URI

internal abstract class AzureAwareClient(
    private val navn: String,
    private val accessTokenClient: AccessTokenClient,
    private val scopes: Set<String>,
    private val pingUrl: URI,
    private val requireAccessAsAppliation: Boolean = true
) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    protected fun authorizationHeader() =
        cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader()

    override suspend fun check() =
        Result.merge(
            navn,
            accessTokenCheck(),
            pingCheck()
        )

    private fun accessTokenCheck() = kotlin.runCatching {
        val accessTokenResponse = accessTokenClient.getAccessToken(scopes)
        when (requireAccessAsAppliation) {
            true -> (SignedJWT.parse(accessTokenResponse.accessToken).jwtClaimsSet.getStringArrayClaim("roles")
                ?.toList()
                ?: emptyList()).contains("access_as_application")
            false -> true
        }
    }.fold(
        onSuccess = {
            when (it) {
                true -> Healthy("AccessTokenCheck", "OK")
                false -> UnHealthy("AccessTokenCheck", "Feil: Mangler rettigheter")
            }
        },
        onFailure = { UnHealthy("AccessTokenCheck", "Feil: ${it.message}") }
    )

    open suspend fun pingCheck(): Result = pingUrl.toString().httpGet {
        it.header(HttpHeaders.AUTHORIZATION, authorizationHeader())
    }.second.fold(
        onSuccess = {
            when (it.status.isSuccess()) {
                true -> Healthy("PingCheck", "OK: ${it.bodyAsText()}")
                false -> UnHealthy("PingCheck", "Feil: ${it.status}")
            }
        },
        onFailure = { UnHealthy("PingCheck", "Feil: ${it.message}") }
    )
}