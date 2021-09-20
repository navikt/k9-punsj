package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.core.Options
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder

fun initWireMock(
        port: Int,
        rootDirectory: String = "../mocks/src/main/resources"
) = WireMockBuilder()
        .withPort(port)
        .withAzureSupport()
        .withNaisStsSupport()
        .wireMockConfiguration {
            it.withRootDirectory(rootDirectory)
            it.useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER)
        }
        .build()
        .stubSaksbehandlerAccessToken()
        .stubNavHeader()
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
        .stubPdlHenteAktøridOk()
        .stubPdlHenteAktøridFinnesIkke()
        .stubPdlHenteAktøridIkkeAutentisert()
        .stubNaisStsTokenResponseGet()
        .stubNaisStsTokenResponsePost()
        .stubNaisStsTokenResponsePut()
        .stubPdlHenteAktøridOkPost()
        .stubGosys()


