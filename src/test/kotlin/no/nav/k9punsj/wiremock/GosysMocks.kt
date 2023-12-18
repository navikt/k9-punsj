package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern

private const val path = "/gosys-mock"

private fun WireMockServer.stubOpprettOppgave(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/api/v1/oppgaver"))
            .withHeader("Authorization", WireMock.matching("Bearer ey.*"))
            .withHeader("X-Correlation-ID", AnythingPattern())
            .withHeader("Content-Type", WireMock.equalTo("application/json"))
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .withHeader("Nav-Consumer-Id", WireMock.equalTo("k9-punsj"))
            .withRequestBody(WireMock.matchingJsonPath("$.aktoerId"))
            .withRequestBody(WireMock.matchingJsonPath("$.journalpostId"))
            .withRequestBody(WireMock.matchingJsonPath("$.tema", WireMock.equalTo("OMS")))
            .withRequestBody(WireMock.matchingJsonPath("$.gjelder", WireMock.absent()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(201)
        )
    )
    return this
}

fun WireMockServer.stubGosys() : WireMockServer =
    stubOpprettOppgave()

fun WireMockServer.getGosysBaseUrl() = baseUrl() + path
