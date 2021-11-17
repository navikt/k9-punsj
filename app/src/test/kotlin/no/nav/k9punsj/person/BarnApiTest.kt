package no.nav.k9punsj.person

import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.awaitExchangeBlocking
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
class BarnApiTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    suspend fun `Person som ikke har noen barn registrert`() {
        @Language("JSON")
        val forventet = """
            {
              "barn": []
            }
        """.trimIndent()

        val hentBarnJson = hentBarnJson("01110050053")
        val body = hentBarnJson.awaitBody<String>()

        JSONAssert.assertEquals(forventet, body, true)
    }

    @Test
    suspend fun `Person som har noen barn registrert`() {
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

        val hentBarnJson = hentBarnJson("66666666666")
        val body = hentBarnJson.awaitBody<String>()

        JSONAssert.assertEquals(forventet, body, true)
    }

    private suspend fun hentBarnJson(identitetsnummer: String): ClientResponse {
        return client.get()
            .uri { it.pathSegment("api", "barn").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", identitetsnummer)
            .awaitExchangeBlocking()
    }
}
