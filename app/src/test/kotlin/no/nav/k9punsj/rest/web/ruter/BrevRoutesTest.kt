package no.nav.k9punsj.rest.web.ruter

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.awaitExchangeBlocking
import no.nav.k9punsj.brev.DokumentbestillingDto
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters


@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class BrevRoutesTest {

    private val client = TestSetup.client
    private val api = "api"
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"


    @Test
    fun `Bestill brev og send til k9-formidling på kafka`() : Unit = runBlocking {
        val norskIdent = "01110050053"
        val journalpostId = "1252334"

        val body = lagBestilling(norskIdent, journalpostId)

        val res = client.post()
            .uri { it.pathSegment(api, "brev", "bestill", journalpostId).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(body))
            .awaitExchangeBlocking()

        Assertions.assertEquals(HttpStatus.OK, res.statusCode())
    }


    private fun lagBestilling(søker: String, journalpostId: String): String {
        return objectMapper().writeValueAsString(
            DokumentbestillingDto(
                journalpostId = journalpostId,
                soekerId = søker,
                mottaker = DokumentbestillingDto.Mottaker("ORGNR", "1231245"),
                fagsakYtelseType = FagsakYtelseType.OMSORGSPENGER,
                dokumentMal = "INNTID"
            )
        )
    }
}
