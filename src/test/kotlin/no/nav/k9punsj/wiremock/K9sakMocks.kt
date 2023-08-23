package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer


private const val path = "/k9sak-mock"


fun WireMockServer.getK9sakBaseUrl() = baseUrl() + path



