package no.nav.k9punsj.person

import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.util.WebClientUtils.awaitExchangeBlocking
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.HttpHeaders
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.awaitBody

@ExtendWith(SpringExtension::class)
class PersonApiTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    suspend fun `Hente person`() {
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


        val body = client.get()
            .uri { it.pathSegment("api", "person").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", identitetsnummer)
            .awaitExchangeBlocking()
            .awaitBody<String>()

        JSONAssert.assertEquals(forventet, body, true)
    }
}
