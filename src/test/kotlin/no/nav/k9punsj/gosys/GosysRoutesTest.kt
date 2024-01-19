package no.nav.k9punsj.gosys

import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.integrasjoner.gosys.Gjelder
import no.nav.k9punsj.wiremock.JournalpostIds
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters

internal class GosysRoutesTest: AbstractContainerBaseTest() {
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `hente gyldige verdier for gjelder`() {
        webTestClient.get()
            .uri{ it.path("/api/gosys/gjelder").build() }
            .exchange()
            .expectStatus().isOk
            .expectBody().json(Gjelder.JSON)
    }

    @Test
    fun `opprette oppgave uten gjelder`() {
        @Language("JSON")
        val body = """
        {
          "journalpostId": "${JournalpostIds.Ok}",
          "norskIdent": "29099000129"
        }
        """.trimIndent()

        webTestClient.post()
            .uri { it.path("/api/gosys/opprettJournalforingsoppgave/").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(body))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `opprette oppgave med gjelder`() {
        @Language("JSON")
        val body = """
        {
          "journalpostId": "${JournalpostIds.Ok}",
          "norskIdent": "29099000129",
          "gjelder": "PleiepengerPårørende"
        }
        """.trimIndent()

        webTestClient.post()
            .uri { it.path("/api/gosys/opprettJournalforingsoppgave/").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(body))
            .exchange()
            .expectStatus().isOk
    }
}
