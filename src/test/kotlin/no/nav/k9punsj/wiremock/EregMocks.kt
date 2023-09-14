package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import org.intellij.lang.annotations.Language

private const val path = "/ereg-mock"

fun WireMockServer.getEregBaseUrl() = baseUrl() + path
fun WireMockServer.stubEreg(): WireMockServer =
    stubHentOrganisasjonNøkkelinformasjon()
        .stubHentOrganisasjonNøkkelinformasjonIkkeFunnet("993110469")
        .stubHentOrganisasjonMedNavn("27500", "QuakeWorld")
        .stubHentOrganisasjonMedNavn("27015", "CounterStrike")
        .stubHentOrganisasjonMedNavn("5001", "Ultima Online")
        .stubHentOrganisasjonMedNavn("2456", "Valheim")


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

private fun WireMockServer.stubHentOrganisasjonMedNavn(
    orgNr: String,
    orgNavn: String
): WireMockServer {
    val navnRespons = """
        {
          "navn": {
            "navnelinje1": "$orgNavn",
            "navnelinje2": "",
            "navnelinje3": null,
            "navnelinje5": "AS"
          }
        }
        """.trimIndent()

    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/organisasjon/$orgNr/noekkelinfo"))
            .withHeader("Nav-Consumer-Id", WireMock.equalTo("k9-punsj"))
            .withHeader("Nav-Call-Id", AnythingPattern())
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(navnRespons)
            )
    )
    return this
}

private fun WireMockServer.stubHentOrganisasjonNøkkelinformasjonIkkeFunnet(
    organisasjonsnummer: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/organisasjon/$organisasjonsnummer/noekkelinfo"))
            .withHeader("Nav-Consumer-Id", WireMock.equalTo("k9-punsj"))
            .withHeader("Nav-Call-Id", AnythingPattern())
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(404)
            )
    )
    return this
}

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
