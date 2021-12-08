package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer


private const val path = "/dokarkiv-mock"

fun WireMockServer.getDokarkivBaseUrl() = baseUrl() + path

