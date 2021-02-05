package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.google.common.net.HttpHeaders.CONTENT_DISPOSITION
import org.intellij.lang.annotations.Language

typealias JournalpostId = String

private const val path = "/saf-mock"

fun WireMockServer.getSafBaseUrl() = baseUrl() + path

fun WireMockServer.stubSafHenteDokumentOk(): WireMockServer {
    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/${JournalpostIds.Ok}.*")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/pdf")
                            .withHeader(CONTENT_DISPOSITION, "inline; filename=${JournalpostIds.Ok}_ARKIV.pdf")
                            .withBodyFile("dummy_soknad.pdf")
                            .withStatus(200)
            )
    )
    return this
}

fun WireMockServer.stubSafHenteDokumentOkForside(): WireMockServer {
    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/201.+")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/pdf")
                            .withHeader(CONTENT_DISPOSITION, "inline; filename=${JournalpostIds.Ok}_ARKIV.pdf")
                            .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/NAV - Førsteside.pdf").readAllBytes())
                            .withStatus(200)
            )
    )
    return this
}


fun WireMockServer.stubSafHenteDokumentOkDelingAvOmsorgsdager(): WireMockServer {
    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/202/470164680.*")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/pdf")
                            .withHeader(CONTENT_DISPOSITION, "inline; filename=${JournalpostIds.Ok}_ARKIV.pdf")
                            .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/NAV - Melding om deling av omsorgsdager.pdf").readAllBytes())
                            .withStatus(200)
            )
    )
    return this
}

fun WireMockServer.stubSafHenteDokumentOkDelingAvOmsorgsdagerSamværserklæring(): WireMockServer {
    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/202/470164681.*")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/pdf")
                            .withHeader(CONTENT_DISPOSITION, "inline; filename=${JournalpostIds.Ok}_ARKIV.pdf")
                            .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/Samværserklæring.pdf").readAllBytes())
                            .withStatus(200)
            )
    )
    return this
}

fun WireMockServer.stubSafHenteDokumentOkEkstraOmsorgsdagerKronisk(): WireMockServer {
    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/203.+")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/pdf")
                            .withHeader(CONTENT_DISPOSITION, "inline; filename=${JournalpostIds.Ok}_ARKIV.pdf")
                            .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/NAV - Søknad om ekstra omsorgsdager ved kronisk sykt eller funksjonshemmet barn.pdf").readAllBytes())
                            .withStatus(200)
            )
    )
    return this
}

fun WireMockServer.stubSafHenteDokumentOkNårArbeidsgiverIkkeBetaler(): WireMockServer {
    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/204.+")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/pdf")
                            .withHeader(CONTENT_DISPOSITION, "inline; filename=${JournalpostIds.Ok}_ARKIV.pdf")
                            .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/NAV - Søknad om utbetaling av omsorgspenger når arbeidsgiver ikke utbetaler.pdf").readAllBytes())
                            .withStatus(200)
            )
    )
    return this
}


private fun WireMockServer.stubSafHenteDokumentError(
    journalpostId: JournalpostId,
    httpStatus: Int
): WireMockServer {
    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/${journalpostId}.*")).willReturn(
                    WireMock.aResponse()
                            .withStatus(httpStatus)
            )
    )
    return this
}

fun WireMockServer.stubSafHenteDokumentNotFound() = stubSafHenteDokumentError(
        journalpostId = JournalpostIds.FinnesIkke,
        httpStatus = 404
)

fun WireMockServer.stubSafHenteDokumentAbacError() = stubSafHenteDokumentError(
        journalpostId = JournalpostIds.AbacError,
        httpStatus = 403
)

private fun WireMockServer.stubSafHenteJournalpost(
    journalpostId: JournalpostId? = null,
    responseBody: String = SafMockResponses.OkResponseHenteJournalpost
): WireMockServer {
    val contentBodyPattern = if (journalpostId == null) AnythingPattern() else ContainsPattern(journalpostId)
    WireMock.stubFor(WireMock.post(WireMock.urlPathMatching(".*$path/graphql.*"))
            .withRequestBody(contentBodyPattern)
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody)
                .withStatus(200)
            )
    )
    return this
}

fun WireMockServer.stubSafHentJournalpostOk() = stubSafHenteJournalpost()
fun WireMockServer.stubSafHentJournalpostAbacError() = stubSafHenteJournalpost(
        journalpostId = JournalpostIds.AbacError,
        responseBody = SafMockResponses.AbacErrorResponseHenteJournalpost
)
fun WireMockServer.stubSafHentJournalpostIkkeKomplettTilgang() = stubSafHenteJournalpost(
        journalpostId = JournalpostIds.IkkeKomplettTilgang,
        responseBody = SafMockResponses.IkkeKomplettTilgangResponseHenteJournalpost
)

fun WireMockServer.stubSafHentJournalpostFinnesIkke() = stubSafHenteJournalpost(
        journalpostId = JournalpostIds.FinnesIkke,
        responseBody = SafMockResponses.FinnesIkkeResponseHenteJournalpost
)

object JournalpostIds {
    const val Ok : JournalpostId = "200"
    const val AbacError : JournalpostId = "500"
    const val IkkeKomplettTilgang : JournalpostId = "403"
    const val FinnesIkke : JournalpostId = "404"

}

private object SafMockResponses {
    @Language("JSON")
    val OkResponseHenteJournalpost = """
    {
      "data": {
        "journalpost": {
          "tema": "OMS",
          "journalposttype": "I",
          "journalstatus": "MOTTATT",
          "bruker": {
            "type": "FNR",
            "id": "29099000129"
          },
          "dokumenter": [
            {
              "dokumentInfoId": "470164680",
              "dokumentvarianter": [
                {
                  "variantformat": "ARKIV",
                  "saksbehandlerHarTilgang": true
                },
                {
                  "variantformat": "ORIGINAL",
                  "saksbehandlerHarTilgang": true
                }
              ]
            },
            {
              "dokumentInfoId": "470164681",
              "dokumentvarianter": [
                {
                  "variantformat": "ARKIV",
                  "saksbehandlerHarTilgang": true
                }
              ]
            }
          ],
          "avsenderMottaker": {
            "id": "29099000129",
            "type": "FNR"
          }
        }
      }
    }
    """.trimIndent()

    @Language("JSON")
    val AbacErrorResponseHenteJournalpost = """
    {
      "errors": [
        {
          "message": "Feilet ved henting av data (/journalpost) : Kunne ikke evaluere tilgang for saksbehandler. Kall mot abac feilet teknisk med statusKode=500 INTERNAL_SERVER_ERROR. Feilmelding=500 Internal Server Error",
          "locations": [
            {
              "line": 3,
              "column": 3
            }
          ],
          "path": [
            "journalpost"
          ],
          "exceptionType": "TECHNICAL",
          "exception": "AbacException"
        }
      ],
      "data": {
        "journalpost": null
      }
    }
    """.trimIndent()

    @Language("JSON")
    val IkkeKomplettTilgangResponseHenteJournalpost = """
    {
      "data": {
        "journalpost": {
          "tema": "OMS",
          "journalposttype": "I",
          "journalstatus": "MOTTATT",
          "bruker": {
            "type": "FNR",
            "id": "29099000129"
          },
          "dokumenter": [
            {
              "dokumentInfoId": "470164680",
              "dokumentvarianter": [
                {
                  "variantformat": "ARKIV",
                  "saksbehandlerHarTilgang": true
                },
                {
                  "variantformat": "ORIGINAL",
                  "saksbehandlerHarTilgang": true
                }
              ]
            },
            {
              "dokumentInfoId": "470164681",
              "dokumentvarianter": [
                {
                  "variantformat": "ARKIV",
                  "saksbehandlerHarTilgang": false
                }
              ]
            }
          ]
        }
      }
    }
    """.trimIndent()

    @Language("JSON")
    val FinnesIkkeResponseHenteJournalpost = """
    {
      "errors": [
        {
          "message": "Feilet ved henting av data (/journalpost) : null",
          "locations": [
            {
              "line": 3,
              "column": 3
            }
          ],
          "path": [
            "journalpost"
          ],
          "exceptionType": "TECHNICAL",
          "exception": "NullPointerException"
        }
      ],
      "data": {
        "journalpost": null
      }
    }
    """.trimIndent()
}
