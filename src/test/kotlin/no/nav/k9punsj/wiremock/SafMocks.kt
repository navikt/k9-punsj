package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import org.intellij.lang.annotations.Language
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION

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
                .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/NAV - Førsteside.pdf")
                    .readAllBytes())
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
                .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/NAV - Melding om deling av omsorgsdager.pdf")
                    .readAllBytes())
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
                .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/Samværserklæring.pdf")
                    .readAllBytes())
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
                .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/NAV - Søknad om ekstra omsorgsdager ved kronisk sykt eller funksjonshemmet barn.pdf")
                    .readAllBytes())
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
                .withBody(this::class.java.getResourceAsStream("/__files/omsorgspenger/NAV - Søknad om utbetaling av omsorgspenger når arbeidsgiver ikke utbetaler.pdf")
                    .readAllBytes())
                .withStatus(200)
        )
    )
    return this
}


private fun WireMockServer.stubSafHenteDokumentError(
    journalpostId: JournalpostId,
    httpStatus: Int,
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/${journalpostId}.*")).willReturn(
            WireMock.aResponse()
                .withStatus(httpStatus)
        )
    )
    return this
}

private fun WireMockServer.stubIkkeStøttet(
    journalpostId: JournalpostId,
    httpStatus: Int,
): WireMockServer {
    WireMock.stubFor( // .urlPathMatching(".*$path/graphql.*"))
        WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/${journalpostId}.*")
        ).willReturn(
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

fun WireMockServer.stubSafIkkeStøttet() = stubSafHenteDokumentError(
    journalpostId = JournalpostIds.IkkeStøttet,
    httpStatus = 409
)

fun WireMockServer.stubSafHenteDokumentAbacError() = stubSafHenteDokumentError(
    journalpostId = JournalpostIds.AbacError,
    httpStatus = 403
)

private fun WireMockServer.stubSafHenteJournalpost(
    journalpostId: JournalpostId? = null,
    responseBody: String = SafMockResponses.OkResponseHenteJournalpost,
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

private fun WireMockServer.stubSafHenteJournalpostPunsjbolle(
    journalpostId: JournalpostId,
    responseBody: String,
): WireMockServer {
    WireMock.stubFor(WireMock.post(WireMock
        .urlPathMatching(".*$path/graphql.*"))
        .withRequestBody(ContainsPattern(journalpostId))
        .withHeader("Nav-Callid", AnythingPattern())
        .willReturn(WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
            .withStatus(200)
        )
    )
    return this
}

fun WireMockServer.stubSafPunsjbolleHentFerdigstillJournalpostOk() = stubSafHenteJournalpostPunsjbolle(
    journalpostId = "7523521",
    responseBody = SafMockResponses.OkResponseHenteFerdigstillJournalpost
)
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
    const val Ok: JournalpostId = "200"
    const val AbacError: JournalpostId = "500"
    const val IkkeKomplettTilgang: JournalpostId = "403"
    const val FinnesIkke: JournalpostId = "404"
    const val IkkeStøttet: JournalpostId = "409"
    const val Utgått: JournalpostId = "1337"

}

private object SafMockResponses {
    @Language("JSON")
    val OkResponseHenteJournalpost = """
    {
      "data": {
        "journalpost": {
          "journalpostId": "123456789",
          "tema": "OMS",
          "journalposttype": "I",
          "journalstatus": "MOTTATT",
          "relevanteDatoer" : [
          {
            "dato" : "2020-10-12T12:53:21.046Z",
            "datotype" : "DATO_REGISTRERT"
          }
          ],
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
              "brevkode": "test",
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
          "message": "Tilgang til ressurs ble avvist. Saksbehandler eller system har ikke tilgang til ressurs tilhørende bruker som har kode 6/7, egen ansatt eller utenfor tillatt geografisk område",
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
          "journalpostId": "123456789",
          "tema": "OMS",
          "journalposttype": "I",
          "journalstatus": "MOTTATT",
          "relevanteDatoer" : [
          {
            "dato" : "2020-10-12T12:53:21.046Z",
            "datotype" : "DATO_REGISTRERT"
          }
          ],
          "bruker": {
            "type": "FNR",
            "id": "29099000129"
          },
          "dokumenter": [
            {
              "dokumentInfoId": "470164680",
              "brevkode": "test",
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
              "brevkode": "test",
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
          "message": "ikke funnet",
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

    @Language("JSON")
    val OkResponseHenteFerdigstillJournalpost = """
    {
      "data": {
        "journalpost": {
          "journalpostId": "7523521",
          "tema": "OMS",
          "journalposttype": "N",
          "journalstatus": "FERDIGSTILT",
          "relevanteDatoer" : [
          {
            "dato" : "2022-10-12T12:53:21.046Z",
            "datotype" : "DATO_REGISTRERT"
          }
          ],
          "bruker": {
            "type": "FNR",
            "id": "02020050123"
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
            }
          ],
          "avsenderMottaker": {
            "id": "02020050123",
            "type": "FNR"
          }
        }
      }
    }
    """.trimIndent()
}
