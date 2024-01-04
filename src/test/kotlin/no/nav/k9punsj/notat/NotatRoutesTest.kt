package no.nav.k9punsj.notat

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.BodyInserters

internal class NotatRoutesTest: AbstractContainerBaseTest() {
    private companion object {
        private val api = "api"
    }

    @Test
    fun `Journalføring av notat`(): Unit = runBlocking {
        val søkerIdent = "66666666666" // no.nav.k9punsj.rest.eksternt.pdl.TestPdlService.harBarn
        val nyNotat = NyNotat(
            søkerIdentitetsnummer = søkerIdent,
            fagsakId = "ABC123",
            tittel = "Journalføring av notat",
            notat = "lorem ipmsum osv..."
        )

        webTestClient.post()
            .uri { it.path("/$api/notat/opprett").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(nyNotat))
            .exchange()
            .expectStatus().isCreated
            .expectBody().jsonPath("$.journalpostId").isEqualTo("201")
    }
}
