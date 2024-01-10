package no.nav.k9punsj.person

import no.nav.k9punsj.AbstractContainerBaseTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

class PersonApiTest: AbstractContainerBaseTest() {

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

        webTestClient.get()
            .uri { it.path("/api/person").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", identitetsnummer)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(forventet)
    }
}
