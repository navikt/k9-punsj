package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import org.intellij.lang.annotations.Language

private const val path = "/ereg-mock"

private fun WireMockServer.stubHentOrganisasjonNøkkelinformasjon(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/organisasjon/.*/noekkelinfo"))
            .withHeader("Nav-Consumer-Id", WireMock.equalTo("k9-punsj"))
            .withHeader("Nav-Call-Id", AnythingPattern())
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(DefaultResponse)
            )
    )
    return this
}

fun WireMockServer.stubEreg() : WireMockServer =
    stubHentOrganisasjonNøkkelinformasjon()

fun WireMockServer.getEregBaseUrl() = baseUrl() + path

@Language("JSON")
private val DefaultResponse = """
{
  "navn": {
    "navnelinje1": "NAV",
    "navnelinje2": "",
    "navnelinje3": null,
    "navnelinje5": "AS"
  }
}
""".trimIndent()
