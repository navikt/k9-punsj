package no.nav.k9.wiremock

import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.WireMockBuilder

internal fun initWireMock(
        port: Int
) = WireMockBuilder()
        .withPort(port)
        .withAzureSupport()
        .build()
        .stubSafHenteDokument()
        .stubSafHenteJounralpost()