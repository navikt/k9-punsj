package no.nav.k9punsj.integrasjoner.arbeidsgivere

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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange

@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class ArbeidsgivereRoutesTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `hente arbeidsgivere for person som har arbeidsgivere`() {
        @Language("JSON")
        val forventetResponse = """
            {
              "organisasjoner": [{
                "organisasjonsnummer": "979312059",
                "navn": "NAV AS"
              }]
            }
        """.trimIndent()
        val (httpStatus, response) = getArbeidsgivere("11111111111")
        assertEquals(HttpStatus.OK, httpStatus)
        JSONAssert.assertEquals(forventetResponse, response, true)
    }

    @Test
    fun `hente arbeidsgivere for person som ikke har arbeidsgivere`() {
        @Language("JSON")
        val forventetResponse = """
            {
              "organisasjoner": []
            }
        """.trimIndent()
        val (httpStatus, response) = getArbeidsgivere("22222222222")
        assertEquals(HttpStatus.OK, httpStatus)
        JSONAssert.assertEquals(forventetResponse, response, true)
    }

    @Test
    fun `hente navn på arbeidsgiver som finnes`() {
        val (httpStatus, response) = getArbeidsgiverNavn("979312059")
        assertEquals(HttpStatus.OK, httpStatus)
        JSONAssert.assertEquals("""{"navn":"NAV AS"}""", response, true)
    }

    @Test
    fun `hente navn på arbeidsgiver som ikke finnes`() {
        val (httpStatus, _) = getArbeidsgiverNavn("993110469")
        assertEquals(HttpStatus.NOT_FOUND, httpStatus)
    }

    private fun getArbeidsgivere(
        identitetsnummer: String
    ): Pair<HttpStatus, String?> = runBlocking {
        client.get()
            .uri { it.path("/api/arbeidsgivere").build() }
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", identitetsnummer)
            .awaitExchange { it.statusCode() to it.awaitBodyOrNull() }
    }

    private fun getArbeidsgiverNavn(organisasjonsnummer: String): Pair<HttpStatus, String?> = runBlocking {
        client.get()
            .uri { it.path("/api/arbeidsgiver").queryParam("organisasjonsnummer", organisasjonsnummer).build() }
            .awaitExchange { it.statusCode() to it.awaitBodyOrNull() }
    }
}
