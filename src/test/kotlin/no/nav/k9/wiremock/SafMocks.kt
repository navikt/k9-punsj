package no.nav.k9.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

private const val path = "/saf-mock"

internal fun WireMockServer.getSafBaseUrl() = baseUrl() + path

internal fun WireMockServer.stubSafHenteDokument(): WireMockServer {
    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$path/rest/hentdokument/.*")).willReturn(
                    WireMock.aResponse()
                            .withHeader("Content-Type", "application/pdf")
                            .withBodyFile("dummy_soknad.pdf")
                            .withStatus(200)
            )
    )
    return this
}
internal fun WireMockServer.stubSafHenteJournalpost(): WireMockServer = this