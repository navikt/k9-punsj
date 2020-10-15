package no.nav.k9.wiremock

import com.github.tomakehurst.wiremock.core.Options
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder

fun initWireMock(
        port: Int,
        rootDirectory: String = "../mocks/src/main/resources"
) = WireMockBuilder()
        .withPort(port)
        .withAzureSupport()
        .wireMockConfiguration {
            it.withRootDirectory(rootDirectory)
            it.useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER)
        }
        .build()
        .stubSaksbehandlerAccessToken()
        .stubSafHenteDokumentOk()
        .stubSafHenteDokumentOkForside()
        .stubSafHenteDokumentOkDelingAvOmsorgsdager()
        .stubSafHenteDokumentOkDelingAvOmsorgsdagerSamværserklæring()
        .stubSafHenteDokumentOkEkstraOmsorgsdagerKronisk()
        .stubSafHenteDokumentOkNårArbeidsgiverIkkeBetaler()
        .stubSafHenteDokumentNotFound()
        .stubSafHenteDokumentAbacError()
        .stubSafHentJournalpostOk()
        .stubSafHentJournalpostAbacError()
        .stubSafHentJournalpostIkkeKomplettTilgang()
        .stubSafHentJournalpostFinnesIkke()