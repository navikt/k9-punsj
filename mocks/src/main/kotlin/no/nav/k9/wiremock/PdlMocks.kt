package no.nav.k9.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching


private const val path = "/pdl-mock"

fun WireMockServer.getPdlBaseUrl() = baseUrl() + path

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
    "{\n" +
            "  \"errors\": [\n" +
            "    {\n" +
            "      \"message\": \"Fant ikke person\",\n" +
            "      \"locations\": [\n" +
            "        {\n" +
            "          \"line\": 7,\n" +
            "          \"column\": 5\n" +
            "        }\n" +
            "      ],\n" +
            "      \"path\": [\n" +
            "        \"hentIdenter\"\n" +
            "      ],\n" +
            "      \"extensions\": {\n" +
            "        \"code\": \"not_found\",\n" +
            "        \"classification\": \"ExecutionAborted\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"data\": {\n" +
            "    \"hentIdenter\": null\n" +
            "  }\n" +
            "}"
    
    """{"errors":[{"message":"Ikke autentisert","locations":[{"line":3,"column":5}],"path":["hentIdenter"],"extensions":{"code":"unauthenticated","classification":"ExecutionAborted"}}],"data":{"hentIdenter":null}}"""
    return this
}

private fun WireMockServer.stubPdlHenteDokumentError(
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

fun WireMockServer.stubPdlHenteDokumentNotFound() = stubPdlHenteDokumentError(
        journalpostId = JournalpostIds.FinnesIkke,
        httpStatus = 404
)

fun WireMockServer.stubPdlHenteDokumentAbacError() = stubPdlHenteDokumentError(
        journalpostId = JournalpostIds.AbacError,
        httpStatus = 403
)

