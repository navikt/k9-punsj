package no.nav.k9punsj.gosys

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange

@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class GosysRoutesTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `hente gyldige verdier for gjelder`() {
        val (statusCode, json) = "api/gosys/gjelder".get()
        assertEquals(HttpStatus.OK, statusCode)
        JSONAssert.assertEquals(Gjelder.JSON, json, true)
    }

    @Test
    fun `opprette oppgave uten gjelder`() {
        @Language("JSON")
        val body = """
        {
          "journalpostId": "202",
          "norskIdent": "29099000129"
        }
        """.trimIndent()

        val (statusCode, _) = "api/gosys/opprettJournalforingsoppgave/".post(body)
        assertEquals(HttpStatus.OK, statusCode)
    }

    @Test
    fun `opprette oppgave med gjelder`() {
        @Language("JSON")
        val body = """
        {
          "journalpostId": "202",
          "norskIdent": "29099000129",
          "gjelder": "PleiepengerPårørende"
        }
        """.trimIndent()

        val (statusCode, _) = "api/gosys/opprettJournalforingsoppgave/".post(body)
        assertEquals(HttpStatus.OK, statusCode)
    }

    private fun String.get(): Pair<HttpStatus, String?> = this.let { path -> runBlocking {
        client.get()
            .uri { it.path(path).build() }
            .awaitExchange { it.statusCode() to it.awaitBodyOrNull() }
    }}

    private fun String.post(body: String): Pair<HttpStatus, String?> = this.let { path -> runBlocking {
        client.post()
            .uri { it.path(path).build() }
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(body))
            .awaitExchange { it.statusCode() to it.awaitBodyOrNull() }
    }}
}