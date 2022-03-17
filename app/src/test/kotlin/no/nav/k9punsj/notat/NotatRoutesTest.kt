package no.nav.k9punsj.notat

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.journalpost.JournalPostResponse
import no.nav.k9punsj.util.postAndAssertAwaitWithStatusAndBody
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters

internal class NotatRoutesTest {
    private companion object {
        private val client = TestSetup.client
        private val api = "api"
        private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    }

    @Test
    fun `Journalføring av notat`(): Unit = runBlocking {
        val søkerIdent = "66666666666" // no.nav.k9punsj.rest.eksternt.pdl.TestPdlService.harBarn
        val body = client.postAndAssertAwaitWithStatusAndBody<NyNotat, JournalPostResponse>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(
                NyNotat(
                    søkerIdentitetsnummer = søkerIdent,
                    fagsakId = "ABC123",
                    tittel = "Journalføring av notat",
                    notat = "lorem ipmsum osv..."
                )
            ),
            api, "notat", "opprett"
        )
        Assertions.assertThat(body.journalpostId).isEqualTo("201")
    }
}
