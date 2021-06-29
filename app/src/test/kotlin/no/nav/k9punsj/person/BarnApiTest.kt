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
class BarnApiTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `Person som ikke har noen barn registrert`() {
        @Language("JSON")
        val forventet = """
            {
              "barn": []
            }
        """.trimIndent()

        JSONAssert.assertEquals(forventet, hentBarnJson("01110050053"), true)
    }

    @Test
    fun `Person som har noen barn registrert`() {
        @Language("JSON")
        val forventet = """
        {
            "barn": [{
                "identitetsnummer": "88888888888",
                "fødselsdato": "2005-12-12",
                "fornavn": "Kari",
                "mellomnavn": "Mellomste",
                "etternavn": "Nordmann",
                "sammensattNavn": "Kari Mellomste Nordmann"
            },{
                "identitetsnummer": "99999999999",
                "fødselsdato": "2004-06-24",
                "fornavn": "Pål",
                "mellomnavn": null,
                "etternavn": "Nordmann",
                "sammensattNavn": "Pål Nordmann"
            }]
        }
        """.trimIndent()

        JSONAssert.assertEquals(forventet, hentBarnJson("66666666666"), true)
    }

    private fun hentBarnJson(identitetsnummer: String) = runBlocking {
        client.get()
            .uri { it.pathSegment("api", "barn").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", identitetsnummer)
            .awaitExchange()
            .awaitBody<String>()
    }
}