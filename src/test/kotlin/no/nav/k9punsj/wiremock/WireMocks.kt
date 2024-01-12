package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.Options
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder

fun initWireMock(
    port: Int? = null,
    rootDirectory: String = "src/test/resources",
): WireMockServer {
    val builder = WireMockBuilder()
        .withAzureSupport()
        .withNaisStsSupport()
        .wireMockConfiguration {
            it.withRootDirectory(rootDirectory)
            it.useChunkedTransferEncoding(Options.ChunkedEncodingPolicy.NEVER)
        }

    port?.let { builder.withPort(it) }

    return builder.build()
        .stubSaf()
        .stubPdl()
        .stubAccessTokens()
        .stubGosys()
        .stubAareg()
        .stubEreg()
        .stubDokarkiv()
}
