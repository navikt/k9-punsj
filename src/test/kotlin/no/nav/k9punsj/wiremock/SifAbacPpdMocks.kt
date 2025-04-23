package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer


private const val path = "/sif-abac-pdp-mock"


fun WireMockServer.getSifAbacPdpBaseUrl() = baseUrl() + path



