package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer


private const val path = "/k9punsjbolle-mock"


fun WireMockServer.getK9PunsjbolleBaseUrl() = baseUrl() + path



