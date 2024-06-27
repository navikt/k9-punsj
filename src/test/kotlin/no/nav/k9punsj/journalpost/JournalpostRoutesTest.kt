package no.nav.k9punsj.journalpost

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.wiremock.stubSafHenteJournalpost
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class JournalpostRoutesTest: AbstractContainerBaseTest() {

    private val api = "api"
    private val journalpostUri = "journalpost"

    @Test
    fun `Gitt journalpost med annet tema enn OMS, forvent conflict feil`(): Unit = runBlocking {
        val journalpostId = "123"

        wireMockServer.stubSafHenteJournalpost(journalpostId, tema = "OPP")

        webTestClient.get()
            .uri("/$api/$journalpostUri/$journalpostId")
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody().json(
                """
                    {"type":"punsj://ikke-støttet-journalpost"}
                """.trimIndent()
            )
    }
}
