package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.jws.NaisSts

private const val path = "/access-token-mock"

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

fun WireMockServer.stubNaisStsTokenResponseGet(): WireMockServer {

    val jwt = Azure.V2_0.saksbehandlerAccessToken()

    WireMock.stubFor(
        WireMock.get(WireMock.urlPathEqualTo("/nais-sts/token")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                                {
                                    "token_type": "Bearer",
                                    "access_token": "$jwt"
                                }
                            """.trimIndent())
                .withStatus(200)
        )
    )
    return this
}

fun WireMockServer.stubNaisStsTokenResponsePost(): WireMockServer {

    val jwt = Azure.V2_0.saksbehandlerAccessToken()

    WireMock.stubFor(
        WireMock.post(WireMock.urlPathEqualTo("/nais-sts/token")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                                {
                                    "token_type": "Bearer",
                                    "access_token": "$jwt"
                                }
                            """.trimIndent())
                .withStatus(200)
        )
    )
    return this
}

fun WireMockServer.stubNaisStsTokenResponsePut(): WireMockServer {

    val jwt = Azure.V2_0.saksbehandlerAccessToken()

    WireMock.stubFor(
        WireMock.put(WireMock.urlPathEqualTo("/nais-sts/token")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                                {   
                                    "token_type": "Bearer",
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
                "sub" to "k9-punsj-frontend-oidc-auth-proxy"
        )
)

fun Azure.V2_0.navHeader() = generateJwt(
        clientId = "nav",
        audience = "k9-punsj"
)

fun NaisSts.k9SakToken() = generateJwt(
    application = "srvk9sak",
    issuer = "naissts",
    overridingClaims = mapOf(
        "sub" to "srvk9sak"
    )
)


