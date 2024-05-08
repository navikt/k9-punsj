package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.ZoneId

private const val path = "/aareg-mock"

fun WireMockServer.stubAareg() : WireMockServer =
    stubHentArbeidsforhold(identitetsnummer = AnythingPattern(), response = defaultResponse)
        .stubHentArbeidsforhold(identitetsnummer = WireMock.equalTo("22222222222"), response = "[]")
        .stubHentArbeidsforhold(identitetsnummer = WireMock.equalTo("22053826656"), response = flereArbeidsforholdIar, inkluderAvsluttetArbeidsforhold = true)

fun WireMockServer.getAaregBaseUrl() = baseUrl() + path

private fun WireMockServer.stubHentArbeidsforhold(
    identitetsnummer: StringValuePattern,
    response: String,
    inkluderAvsluttetArbeidsforhold: Boolean = false
): WireMockServer {
    val builder = WireMock.get(WireMock.urlPathMatching(".*$path/arbeidstaker/arbeidsforhold.*"))
        .withHeader("Authorization", WireMock.matching("Bearer ey.*"))
        .withHeader("Nav-Call-Id", AnythingPattern())
        .withHeader("Accept", WireMock.equalTo("application/json"))
        .withHeader("Nav-Personident", identitetsnummer)
        .withQueryParam("sporingsinformasjon", WireMock.equalTo("false"))
        .withQueryParam("rapporteringsordning", WireMock.equalTo("A_ORDNINGEN"))

    if (inkluderAvsluttetArbeidsforhold) {
        builder.withQueryParam("arbeidsforholdstatus", WireMock.havingExactly("AKTIV", "AVSLUTTET", "FREMTIDIG"))
    }

    WireMock.stubFor(
        builder
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(response)
            )
    )
    return this
}

private val fom =  LocalDate.now(ZoneId.of("Europe/Oslo")).minusMonths(6)
private val tom =  LocalDate.now(ZoneId.of("Europe/Oslo")).plusMonths(6)
private val idag = LocalDate.now(ZoneId.of("Europe/Oslo"))

@Language("JSON")
private val defaultResponse = """
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

@Language("JSON")
private val flereArbeidsforholdIar = """
[
    {
        "id": "QuakeWorld",
        "arbeidssted": {
            "type": "Organisasjon",
            "identer": [
                {
                  "ident": "27500",
                  "type": "ORGANISASJONSNUMMER"
                }
            ]
        },
        "ansettelsesperiode": {
          "startdato": "$idag",
          "sluttdato": null
        }
    },
    {
        "id": "CounterStrike",
        "arbeidssted": {
            "type": "Organisasjon",
            "identer": [
                {
                  "ident": "27015",
                  "type": "ORGANISASJONSNUMMER"
                }
            ]
        },
        "ansettelsesperiode": {
          "startdato": "${idag.minusMonths(2)}",
          "sluttdato": "${idag.minusMonths(1)}"
        }
    }, 
    {
        "id": "Ultima Online",
        "arbeidssted": {
            "type": "Organisasjon",
            "identer": [
                {
                  "ident": "5001",
                  "type": "ORGANISASJONSNUMMER"
                }
            ]
        },
        "ansettelsesperiode": {
          "startdato": "${idag.minusMonths(4)}",
          "sluttdato": "${idag.minusMonths(3)}"
        }
    },
    {
        "id": "Ultima Online 2",
        "arbeidssted": {
            "type": "Organisasjon",
            "identer": [
                {
                  "ident": "5002",
                  "type": "AKTORID"
                }
            ]
        },
        "ansettelsesperiode": {
          "startdato": "${idag.minusMonths(4)}",
          "sluttdato": "${idag.minusMonths(3)}"
        }
    },
    {
        "id": "Valheim",
        "arbeidssted": {
            "type": "Organisasjon",
            "identer": [
                {
                  "ident": "2456",
                  "type": "ORGANISASJONSNUMMER"
                }
            ]
        },
        "ansettelsesperiode": {
          "startdato": "${idag.minusMonths(8)}",
          "sluttdato": "${idag.minusMonths(7)}"
        }
    }
]
""".trimIndent()
