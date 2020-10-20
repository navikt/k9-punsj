package no.nav.k9.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.google.common.net.HttpHeaders.CONTENT_DISPOSITION
import org.intellij.lang.annotations.Language


private const val path = "/pdl-mock"

fun WireMockServer.getPdlBaseUrl() = baseUrl() + path

fun WireMockServer.stubPdlHenteAktøridOk(): WireMockServer {
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

private object PdlMockResponses {
    @Language("JSON")
    val OkResponseHentIdent = """
    
    """.trimIndent()

    @Language("JSON")
    val AbacErrorHentIdent = """
   
    """.trimIndent()

    @Language("JSON")
    val FinnesIkkeHentIdent = """
   
    """.trimIndent()
}