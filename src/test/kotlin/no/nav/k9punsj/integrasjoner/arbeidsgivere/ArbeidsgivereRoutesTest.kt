package no.nav.k9punsj.integrasjoner.arbeidsgivere

import no.nav.k9punsj.AbstractContainerBaseTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

internal class ArbeidsgivereRoutesTest : AbstractContainerBaseTest() {

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

        webTestClient.get()
            .uri { it.path("/api/arbeidsgivere").build() }
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", "11111111111")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(forventetResponse)
    }

    @Test
    fun `hente arbeidsgivere for person som ikke har arbeidsgivere`() {
        @Language("JSON")
        val forventetResponse = """
            {
              "organisasjoner": []
            }
        """.trimIndent()
        webTestClient.get()
            .uri { it.path("/api/arbeidsgivere").build() }
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", "22222222222")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(forventetResponse)
    }

    @Test
    fun `hente navn på arbeidsgiver som finnes`() {
        webTestClient.get()
            .uri { it.path("/api/arbeidsgiver").queryParam("organisasjonsnummer", "979312059").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isOk
            .expectBody().json("""{"navn":"NAV AS"}""")
    }

    @Test
    fun `hente navn på arbeidsgiver som ikke finnes`() {
        webTestClient.get()
            .uri { it.path("/api/arbeidsgiver").queryParam("organisasjonsnummer", "993110469").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `henter historiske arbeidsgivere fra siste 6 mån, tar kun med de som har orgnr`() {
        val forventetResponse = """
            {
              "organisasjoner": [
                  {
                    "organisasjonsnummer": "27500",
                    "navn": "QuakeWorld AS"
                  },
                  {
                    "organisasjonsnummer": "27015",
                    "navn": "CounterStrike AS"
                  },
                  {
                    "organisasjonsnummer": "5001",
                    "navn": "Ultima Online AS"
                  },
              ]
            }
        """.trimIndent()

        webTestClient.get()
            .uri {
                it.path("/api/arbeidsgivere").queryParam("inkluderAvsluttetArbeidsforhold", "true").build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", "22053826656")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(forventetResponse)
    }
}
