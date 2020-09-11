package no.nav.k9.wiremock

import com.github.tomakehurst.wiremock.core.Options
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder

internal fun initWireMock(
        port: Int
) = WireMockBuilder()
        .withPort(port)
        .withAzureSupport()
        .wireMockConfiguration {
            it.withRootDirectory("src/test/resources")
            it.useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER)
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