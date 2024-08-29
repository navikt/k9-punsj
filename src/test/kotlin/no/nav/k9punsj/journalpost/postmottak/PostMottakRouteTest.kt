package no.nav.k9punsj.journalpost.postmottak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.CALL_ID_KEY
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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

        coEvery { postMottakService.klassifiserOgJournalfør(any()) } throws PostMottakException(
            melding = "Det eksisterer allerede en fagsak(PLEIEPENGER_SYKT_BARN - ABC123) på pleietrengende.",
            httpStatus = HttpStatus.CONFLICT,
            journalpostId = journalpostId
        )

        val mottaksHaandteringDto = JournalpostMottaksHaandteringDto(
            journalpostId = journalpostId,
            brukerIdent = brukerIdent,
            pleietrengendeIdent = "456",
            fagsakYtelseTypeKode = PunsjFagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode,
            saksnummer = null,
            relatertPersonIdent = null
        )

        webTestClient
            .post()
            .uri { it.path("/api/journalpost/mottak").build() }
            .body(BodyInserters.fromValue(mottaksHaandteringDto))
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", mottaksHaandteringDto.pleietrengendeIdent)
            .header(CALL_ID_KEY, correlationId)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody()
            .json(
                //language=JSON
                """
                {
                  "type": "/problem-details/post-mottak",
                  "title": "Feil ved journalføring av journalpost $journalpostId",
                  "status": 409,
                  "detail": "Det eksisterer allerede en fagsak(PLEIEPENGER_SYKT_BARN - ABC123) på pleietrengende.",
                  "instance": "/api/journalpost/mottak",
                  "correlationId": "$correlationId"
                }
                """.trimIndent(), true
            )
    }


    @ParameterizedTest
    @ValueSource(strings = ["1999", "2101", "null"])
    fun `forventer feil dersom behandlingsår er ugyldig for OMP`(behandlingsår: String?): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val brukerIdent = "123"

        assertThrows<PostMottakException> { JournalpostMottaksHaandteringDto(
            journalpostId = journalpostId,
            brukerIdent = brukerIdent,
            pleietrengendeIdent = "456",
            fagsakYtelseTypeKode = PunsjFagsakYtelseType.OMSORGSPENGER.kode,
            saksnummer = null,
            relatertPersonIdent = null
        ).valider(behandlingsår?.toIntOrNull()) }
    }
}
