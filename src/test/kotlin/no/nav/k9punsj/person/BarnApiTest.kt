package no.nav.k9punsj.person

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

class BarnApiTest : AbstractContainerBaseTest() {
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `Person som ikke har noen barn registrert`(): Unit = runBlocking {
        @Language("JSON")
        val forventet = """
            {
              "barn": []
            }
        """.trimIndent()

        webTestClient.get()
            .uri { it.pathSegment("api", "barn").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", "01110050053")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(forventet)
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

        webTestClient.get()
            .uri { it.pathSegment("api", "barn").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", "66666666666")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(forventet)
    }
}
