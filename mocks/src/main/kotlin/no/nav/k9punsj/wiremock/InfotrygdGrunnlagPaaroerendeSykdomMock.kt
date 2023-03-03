package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import no.nav.k9punsj.wiremock.WireMockVerktøy.withAuthorizationHeader
import no.nav.k9punsj.wiremock.WireMockVerktøy.withJson
import no.nav.k9punsj.wiremock.WireMockVerktøy.withNavPostHeaders

private const val path = "/infotrygd-grunnlag-paaroerende-sykdom-mock"

private fun WireMockServer.mockPingUrl(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path/actuator/health")).withAuthorizationHeader()
            .willReturn(WireMock.aResponse().withStatus(200)))
    return this
}

private fun WireMockServer.mockSaker(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/saker.*"))
            .withNavPostHeaders().withRequestBody(AnythingPattern())
            .willReturn(WireMock.aResponse().withJson("""[{"saker":[], "vedtak":[]}]""")))
    return this
}

private fun WireMockServer.mockVedtakForPleietrengende(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/vedtakForPleietrengende.*"))
            .withNavPostHeaders().withRequestBody(AnythingPattern())
            .willReturn(WireMock.aResponse().withJson("""[{"vedtak":[]}]""")))
    return this
}

internal fun WireMockServer.mockInfotrygdGrunnlagPaaroerendeSykdom() = mockPingUrl().mockSaker().mockVedtakForPleietrengende()
fun WireMockServer.getInfotrygdGrunnlagPaaroerendeSykdomBaseUrl() = baseUrl() + path