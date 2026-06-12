package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.helse.dusseldorf.testsupport.jws.Azure

private const val path = "/access-token-mock"
fun WireMockServer.stubAccessTokens() = stubSaksbehandlerAccessToken()
    .stubNavHeader()

fun WireMockServer.stubSaksbehandlerAccessToken(): WireMockServer {

    val jwt = Azure.V2_0.saksbehandlerAccessToken()

    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/saksbehandler.*")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "access_token": "$jwt"
                                }
                            """.trimIndent())
                            .withStatus(200)
            )
    )
    return this
}

fun WireMockServer.stubNavHeader(): WireMockServer {

    val jwt = Azure.V2_0.navHeader()

    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/navHeader.*")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""
                                {
                                    "access_token": "$jwt"
                                }
                            """.trimIndent())
                            .withStatus(200)
            )
    )
    return this
}

fun Azure.V2_0.saksbehandlerAccessToken() = generateJwt(
        clientId = "k9-punsj-frontend-oidc-auth-proxy",
        audience = "k9-punsj",
        overridingClaims = mapOf(
                "sub" to "k9-punsj-frontend-oidc-auth-proxy",
                "NAVident" to "Z000000",
                "oid" to "00000000-0000-0000-0000-000000000000",
                "scp" to "defaultaccess",
                "azp" to "k9-punsj-frontend-oidc-auth-proxy",
                "groups" to emptyList<String>()
        )
)

fun Azure.V2_0.navHeader() = generateJwt(
        clientId = "nav",
        audience = "k9-punsj"
)
