package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import org.intellij.lang.annotations.Language


private const val path = "/pdl-mock"

typealias AktørId = String

fun WireMockServer.getPdlBaseUrl() = baseUrl() + path

object AktørIds {
    const val Ok : AktørId = "200"
    const val AbacError : AktørId = "500"
    const val IkkeKomplettTilgang : AktørId = "403"
    const val FinnesIkke : AktørId = "404"

}

fun WireMockServer.stubPdlHenteAktøridOk(): WireMockServer {
    WireMock.stubFor(
            WireMock.get(urlPathMatching(".*$path/graphql")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\n" +
                                    "  \"data\": {\n" +
                                    "    \"hentIdenter\": {\n" +
                                    "      \"identer\": [\n" +
                                    "        {\n" +
                                    "          \"ident\": \"2002220522526\",\n" +
                                    "          \"historisk\": false,\n" +
                                    "          \"gruppe\": \"AKTORID\"\n" +
                                    "        }\n" +
                                    "      ]\n" +
                                    "    }\n" +
                                    "  }\n" +
                                    "}")
                            .withStatus(200)
            )
    )
    return this
}

fun WireMockServer.stubPdlHenteAktøridOkPost(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(urlEqualTo("/pdl-mock")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\n" +
                        "  \"data\": {\n" +
                        "    \"hentIdenter\": {\n" +
                        "      \"identer\": [\n" +
                        "        {\n" +
                        "          \"ident\": \"2002220522526\",\n" +
                        "          \"historisk\": false,\n" +
                        "          \"gruppe\": \"AKTORID\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  }\n" +
                        "}")
                .withStatus(200)
        )
    )
    return this
}



private fun WireMockServer.stubPdlHenteAktørId(
    aktørId: AktørId? = null,
    responseBody: String = PdlMockResponses.OkResponseHenteAktørId
): WireMockServer {
    val contentBodyPattern = if (aktørId == null) AnythingPattern() else ContainsPattern(aktørId)
    WireMock.stubFor(WireMock.post(urlPathMatching(".*$path/graphql.*"))
            .withRequestBody(contentBodyPattern)
            .willReturn(WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody)
                    .withStatus(200)
            )
    )
    return this
}

private fun WireMockServer.stubPdlHenteAktøridError(httpStatus: Int) : WireMockServer {
    WireMock.stubFor(
            WireMock.get(urlPathMatching(".*$path/graphql")).willReturn(
                    WireMock.aResponse()
                            .withStatus(httpStatus)
            )
    )
    return this
}

fun WireMockServer.stubPdlHenteAktøridFinnesIkke() = stubPdlHenteAktørId(
        aktørId = AktørIds.FinnesIkke,
        responseBody = PdlMockResponses.FinnesIkkeResponseHenteAktørId
)

fun WireMockServer.stubPdlHenteAktøridIkkeAutentisert() = stubPdlHenteAktørId(
        aktørId = AktørIds.FinnesIkke,
        responseBody = PdlMockResponses.IkkeAutentisertResponseHenteAktørId
)

private object PdlMockResponses {
    @Language("JSON")
    val OkResponseHenteAktørId = """
    {
      "data": {
        "hentIdenter": {
          "identer": [
          {
              "ident": "2002220522526",
              "historisk": false,
              "gruppe": "AKTORID"
          }
          ]
        }
      }
    }
    """.trimIndent()


    @Language("JSON")
    val FinnesIkkeResponseHenteAktørId = """
    {
      "errors": [
        {
          "message": "Fant ikke person",
          "locations": [
            {
              "line": 7,
              "column": 5
            }
          ],
          "path": [
            "hentIdenter"
          ],
          "extensions": {
            "code": "not_found",
            "classification": "ExecutionAborted"
          }
        }
      ],
      "data": {
        "hentIdenter": null
      }
    }
    """.trimIndent()

    @Language("JSON")
    val IkkeAutentisertResponseHenteAktørId = """
    {
      "errors": [
        {
          "message": "Ikke autentisert",
          "locations": [
            {
              "line": 3,
              "column": 5
            }
          ],
          "path": [
            "hentIdenter"
          ],
          "extensions": {
            "code": "unauthenticated",
            "classification": "ExecutionAborted"
          }
        }
      ],
      "data": {
        "hentIdenter": null
      }
    }
    """.trimIndent()
}

