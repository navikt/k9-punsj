package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.ZoneId

private const val path = "/aareg-mock"

private fun WireMockServer.stubHentArbeidsforhold(
    identitetsnummer: StringValuePattern,
    response: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/arbeidstaker/arbeidsforhold.*"))
            .withHeader("Authorization", WireMock.matching("Bearer ey.*"))
            .withHeader("Nav-Consumer-Token", WireMock.matching("Bearer ey.*"))
            .withHeader("Nav-Call-Id", AnythingPattern())
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .withHeader("Nav-Personident", identitetsnummer)
            .withQueryParam("historikk", WireMock.equalTo("false"))
            .withQueryParam("sporingsinformasjon", WireMock.equalTo("false"))
            .withQueryParam("rapporteringsordning", WireMock.equalTo("A_ORDNINGEN"))
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(response)
            )
    )
    return this
}

fun WireMockServer.stubAareg() : WireMockServer =
    stubHentArbeidsforhold(identitetsnummer = AnythingPattern(), response = DefaultResponse)
    .stubHentArbeidsforhold(identitetsnummer = WireMock.equalTo("22222222222"), response = "[]")

fun WireMockServer.getAaregBaseUrl() = baseUrl() + path

val fom =  LocalDate.now(ZoneId.of("Europe/Oslo")).minusMonths(6)
val tom =  LocalDate.now(ZoneId.of("Europe/Oslo")).plusMonths(6)

@Language("JSON")
private val DefaultResponse = """
[
    {
        "id": "979312059-arbf-1",
        "arbeidssted": {
            "type": "Organisasjon",
            "identer": [
                {
                  "ident": "979312059",
                  "type": "ORGANISASJONSNUMMER"
                }
            ]
        },
        "ansettelsesperiode": {
          "startdato": "$fom",
          "sluttdato": null
        }
    },
    {
        "id": "979312059-arbf-2",
        "arbeidssted": {
            "type": "Organisasjon",
            "identer": [
                {
                  "ident": "979312059",
                  "type": "ORGANISASJONSNUMMER"
                }
            ]
        },
        "ansettelsesperiode": {
          "startdato": "${fom.minusYears(1)}",
          "sluttdato": "${tom.minusYears(1)}"
        }
    }, 
    {
        "id": "privat-arbef",
        "arbeidssted": {
            "type": "Organisasjon",
            "identer": [
                {
                  "ident": "1234",
                  "type": "AKTORID"
                }
            ]
        },
        "ansettelsesperiode": {
          "startdato": "${fom.minusYears(2)}",
          "sluttdato": "${tom.minusYears(2)}"
        }
    }
]
""".trimIndent()
