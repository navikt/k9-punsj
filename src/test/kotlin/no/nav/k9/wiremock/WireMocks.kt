package no.nav.k9.wiremock

import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder

internal fun initWireMock(
        port: Int
) = WireMockBuilder()
        .withPort(port)
        .withAzureSupport()
        .wireMockConfiguration {
            it.withRootDirectory("src/test/resources")
        }
        .build()
        .stubSaksbehandlerAccessToken()
        .stubSafHenteDokumentOk()
        .stubSafHenteDokumentNotFound()
        .stubSafHenteDokumentAbacError()
        .stubSafHentJournalpostOk()
        .stubSafHentJournalpostAbacError()
        .stubSafHentJournalpostIkkeKomplettTilgang()
        .stubSafHentJournalpostFinnesIkke()