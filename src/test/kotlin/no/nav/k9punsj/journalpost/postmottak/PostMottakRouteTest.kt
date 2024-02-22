package no.nav.k9punsj.journalpost.postmottak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.CALL_ID_KEY
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.integrasjoner.k9sak.dto.Fagsak
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters
import java.util.*

internal class PostMottakRouteTest : AbstractContainerBaseTest() {

    @MockkBean
    private lateinit var postMottakService: PostMottakService


    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @BeforeEach
    internal fun setUp() {
    }

    @AfterEach
    fun tearDown() {
        cleanUpDB()
    }

    @Test
    fun `forventer feil ved eksisterende fagsak på pleietrengendeIdent`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val correlationId = UUID.randomUUID().toString()
        val brukerIdent = "123"

        coEvery { postMottakService.klassifiserOgJournalfør(any()) } throws EksisterendeFagsakPåPleietrengendeException(
            journalpostId, Fagsak(
                saksnummer = "ABC123",
                sakstype = PLEIEPENGER_SYKT_BARN,
                pleietrengendeAktorId = "456",
                gyldigPeriode = null
            )
        )

        val mottaksHaandteringDto = JournalpostMottaksHaandteringDto(
            journalpostId = journalpostId,
            brukerIdent = brukerIdent,
            barnIdent = "456",
            fagsakYtelseTypeKode = FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode,
            saksnummer = null
        )

        webTestClient
            .post()
            .uri { it.path("/api/journalpost/mottak").build() }
            .body(BodyInserters.fromValue(mottaksHaandteringDto))
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", mottaksHaandteringDto.barnIdent)
            .header(CALL_ID_KEY, correlationId)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody()
            .json(
                //language=JSON
                """
                {
                  "type": "/problem-details/eksisterende-fagsak-på-pleietrengende",
                  "title": "Eksisterende fagsak på pleietrengende",
                  "status": 409,
                  "detail": "Det eksisterer allerede en fagsak(PLEIEPENGER_SYKT_BARN - ABC123) på pleietrengende. JournalpostId = $journalpostId.",
                  "instance": "/api/journalpost/mottak",
                  "correlationId": "$correlationId"
                }
                """.trimIndent(), true
            )
    }
}
