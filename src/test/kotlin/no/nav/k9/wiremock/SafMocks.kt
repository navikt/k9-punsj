package no.nav.k9.wiremock

import com.github.tomakehurst.wiremock.WireMockServer

internal fun WireMockServer.getSafBaseUrl() = baseUrl() + "/saf-mock"
internal fun WireMockServer.stubSafHenteDokument(): WireMockServer = this
internal fun WireMockServer.stubSafHenteJounralpost(): WireMockServer = this