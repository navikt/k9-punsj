package no.nav.k9punsj.person

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.awaitBodyWithType
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.HttpHeaders
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class BarnApiTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `Person som ikke har noen barn registrert`(): Unit = runBlocking {
        @Language("JSON")
        val forventet = """
            {
              "barn": []
            }
        """.trimIndent()

        val hentBarnJson = hentBarnJson("01110050053")
        JSONAssert.assertEquals(forventet, hentBarnJson, true)
    }

    @Test
    fun `Person som har noen barn registrert`(): Unit = runBlocking {
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
        JSONAssert.assertEquals(forventet, hentBarnJson, true)
    }

    private suspend fun hentBarnJson(identitetsnummer: String): String {
        return client.get()
            .uri { it.pathSegment("api", "barn").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", identitetsnummer)
            .awaitBodyWithType()
    }
}
