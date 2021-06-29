package no.nav.k9punsj.person

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.HttpHeaders
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@ExtendWith(SpringExtension::class)
class PersonApiTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `Hente person`() {
        val identitetsnummer = "66666666666"

        @Language("JSON")
        val forventet = """
        {
            "identitetsnummer": "$identitetsnummer",
            "fødselsdato": "1980-05-06",
            "fornavn": "Søker",
            "mellomnavn": null,
            "etternavn": "Søkersen",
            "sammensattNavn": "Søker Søkersen"
        }
        """.trimIndent()

        JSONAssert.assertEquals(forventet, hentPersonJson(identitetsnummer), true)
    }

    private fun hentPersonJson(identitetsnummer: String) = runBlocking {
        client.get()
            .uri { it.pathSegment("api", "person").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", identitetsnummer)
            .awaitExchange()
            .awaitBody<String>()
    }
}