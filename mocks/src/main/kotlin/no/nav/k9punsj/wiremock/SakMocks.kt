package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer


private const val path = "/sak-mock"


fun WireMockServer.getSakBaseUrl() = baseUrl() + path



