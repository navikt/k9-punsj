package no.nav.k9punsj.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock


private const val path = "/dokarkiv-mock"

fun WireMockServer.getDokarkivBaseUrl() = baseUrl() + path

fun WireMockServer.stubJournalføringAvNotat(
    responsStatus: Int = 201
) : WireMockServer{
    WireMock.stubFor(
        WireMock.post(
            WireMock.urlPathMatching("$path/rest/journalpostapi/v1/journalpost"))
            .withHeader("Nav-Consumer-Id", WireMock.equalTo("k9-punsj"))
            .withHeader("Nav-Callid", WireMock.matching(".*"))
            .withHeader("Authorization", WireMock.matching(".*"))
            .withRequestBody(WireMock.matchingJsonPath("$.journalposttype", WireMock.matching("NOTAT")))
            .withRequestBody(WireMock.matchingJsonPath("$.kanal", WireMock.matching("NAV_NO|ALTINN|EESSI|INGEN_DISTRIBUSJON")))
            .withRequestBody(WireMock.matchingJsonPath("$.tema", WireMock.equalTo("OMS")))
            .withRequestBody(WireMock.matchingJsonPath("$.eksternReferanseId", WireMock.matching(".*")))
            .withRequestBody(WireMock.matchingJsonPath("$.journalfoerendeEnhet", WireMock.equalTo("9999")))
            .withRequestBody(WireMock.matchingJsonPath("$.sak.fagsakId", WireMock.matching(".*")))
            .withRequestBody(WireMock.matchingJsonPath("$.sak.fagsaksystem", WireMock.equalTo("K9")))
            .withRequestBody(WireMock.matchingJsonPath("$.sak.sakstype", WireMock.equalTo("FAGSAK")))
            .withRequestBody(WireMock.matchingJsonPath("$.bruker.id", WireMock.matching(".*")))
            .withRequestBody(WireMock.matchingJsonPath("$.bruker.idType", WireMock.equalTo("FNR")))
            .withRequestBody(WireMock.matchingJsonPath("$.avsenderMottaker.navn", WireMock.matching(".*")))
            .withRequestBody(WireMock.matchingJsonPath("$.tilleggsopplysninger[0].nokkel", WireMock.equalTo("inneholderSensitivePersonopplysninger")))
            .withRequestBody(WireMock.matchingJsonPath("$.tilleggsopplysninger[0].verdi", WireMock.matching("true|false")))
            .withRequestBody(WireMock.matchingJsonPath("$.dokumenter[0].tittel", WireMock.matching(".*")))
            .withRequestBody(WireMock.matchingJsonPath("$.dokumenter[0].brevkode", WireMock.equalTo("K9_PUNSJ_INNSENDING")))
            .withRequestBody(WireMock.matchingJsonPath("$.dokumenter[0].dokumentVarianter[0].filtype", WireMock.equalTo("PDFA")))
            .withRequestBody(WireMock.matchingJsonPath("$.dokumenter[0].dokumentVarianter[0].variantformat", WireMock.equalTo("ARKIV")))
            .withRequestBody(WireMock.matchingJsonPath("$.dokumenter[0].dokumentVarianter[0].fysiskDokument", WireMock.matching(".*")))
            .withRequestBody(WireMock.matchingJsonPath("$.dokumenter[0].dokumentVarianter[1].filtype", WireMock.equalTo("JSON")))
            .withRequestBody(WireMock.matchingJsonPath("$.dokumenter[0].dokumentVarianter[1].variantformat", WireMock.equalTo("ORIGINAL")))
            .withRequestBody(WireMock.matchingJsonPath("$.dokumenter[0].dokumentVarianter[1].fysiskDokument", WireMock.matching(".*")))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(responsStatus)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        //language=json
                        """
                        {
                          "journalpostId": "201"
                        }
                    """.trimIndent())
                    //.withTransformers("dokarkiv")
            )
    )
    return this
}

