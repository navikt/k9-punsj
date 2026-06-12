package no.nav.k9punsj.journalpost

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.sak.SakService
import no.nav.k9punsj.sak.dto.SakInfoDto
import no.nav.k9punsj.wiremock.SafMockResponses
import no.nav.k9punsj.wiremock.stubSafHenteJournalpost
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class JournalpostRoutesTest: AbstractContainerBaseTest() {

    private val api = "api"
    private val journalpostUri = "journalpost"

    @MockkBean
    private lateinit var sakService: SakService

    @Test
    fun `Gitt journalpost med annet tema enn OMS, forvent conflict feil`(): Unit = runBlocking {
        val journalpostId = "123"

        gittJournalpostMedTema(journalpostId = journalpostId, tema = "SYK")

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

    @Test
    fun `Gitt journalpost knyttet til historisk sak og bruker uten historisk tilgang, forvent 403`(): Unit = runBlocking {
        val journalpostId = "321"
        val fagsakId = "HISTORISK123"
        val norskIdent = "24420167209"

        wireMockServer.stubSafHenteJournalpost(
            journalpostId = journalpostId,
            responseBody = SafMockResponses.OkResponseHenteJournalpost(
                journalpostId = journalpostId,
                tema = "OMS",
                fagsakId = fagsakId
            )
        )

        coEvery { sakService.hentSaker(any()) } returns listOf(
            SakInfoDto(
                reservert = false,
                fagsakId = fagsakId,
                sakstype = "PSB",
                pleietrengendeIdent = null,
                pleietrengende = null,
                gyldigPeriode = PeriodeDto(fom = java.time.LocalDate.of(2020, 1, 1), tom = java.time.LocalDate.of(2020, 12, 31)),
                behandlingsår = 2020,
                relatertPersonIdent = null,
                relatertPerson = null,
                historisk = true,
            )
        )

        webTestClient.get()
            .uri("/$api/$journalpostUri/$journalpostId")
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
            .expectBody(String::class.java)
            .isEqualTo("Fagsak $fagsakId er historisk, og brukeren har ikke tilgang til historiske saker.")
    }

    private fun gittJournalpostMedTema(journalpostId: String, tema: String) {
        wireMockServer.stubSafHenteJournalpost(
            journalpostId = journalpostId,
            responseBody = SafMockResponses.OkResponseHenteJournalpost(tema = tema)
        )
    }
}
