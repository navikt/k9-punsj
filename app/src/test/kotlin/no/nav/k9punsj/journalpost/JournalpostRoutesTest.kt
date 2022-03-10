package no.nav.k9punsj.journalpost

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.journalpost.JournalpostRoutes.Companion.hentBareKodeverdien
import no.nav.k9punsj.util.postAndAssertAwaitWithStatusAndBody
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters

@ExtendWith(SpringExtension::class, MockKExtension::class)
class JournalpostRoutesTest {

    private companion object {
        private val client = TestSetup.client
        private val api = "api"
        private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    }

    @Test
    internal fun testHentKodeVerdi() {
        val hentBareKodeverdien = "2970 NAV IKT DRIFT".hentBareKodeverdien()
        Assertions.assertThat(hentBareKodeverdien).isEqualTo("2970")
    }

    @Test
    fun `Journalføring av notat`(): Unit = runBlocking {
        val søkerIdent = "02022352122"
        val body = client.postAndAssertAwaitWithStatusAndBody<NyJournalpost, JournalPostResponse>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(
                NyJournalpost(
                    søkerIdentitetsnummer = søkerIdent,
                    søkerNavn = "Trane Kreativ",
                    fagsakId = "ABC123",
                    tittel = "Journalføring av notat",
                    notat = "lorem ipmsum osv...",
                    inneholderSensitivePersonopplysninger = true
                )
            ),
            api, "journalpost", "opprett"
        )
        assertThat(body.journalpostId).isEqualTo("201")
    }
}





