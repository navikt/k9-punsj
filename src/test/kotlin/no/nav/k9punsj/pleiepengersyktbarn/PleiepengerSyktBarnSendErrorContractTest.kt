package no.nav.k9punsj.pleiepengersyktbarn

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.CALL_ID_KEY
import no.nav.k9punsj.felles.RestKallException
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.wiremock.JournalpostIds
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.util.UUID

internal class PleiepengerSyktBarnSendErrorContractTest : AbstractContainerBaseTest() {

    @MockkBean
    private lateinit var k9SakService: K9SakService

    private val api = "api"
    private val søknadTypeUri = "pleiepenger-sykt-barn-soknad"

    @AfterEach
    fun teardown() {
        cleanUpDB()
    }

    @Test
    fun `PSB send returnerer normalized detail ved JSON upstream-feil`(): Unit = runBlocking {
        val correlationId = UUID.randomUUID().toString()
        val norskIdent = "02020050121"
        val sendSøknad = opprettUtfyltSøknad(
            norskIdent = norskIdent,
            journalpostId = JournalpostIds.FerdigstiltMedSaksnummer
        )

        val upstreamDetail = """{"feilmelding":"Det oppstod en serverfeil","feilkode":null,"type":"GENERELL_FEIL"}"""

        coEvery { k9SakService.hentPerioderSomFinnesIK9ForSaksnummer(any()) } returns Pair(emptyList(), null)
        coEvery { k9SakService.opprettSakOgSendInnSøknad(any(), any(), any(), any(), any(), any()) } throws RestKallException(
            titel = "Restkall mot k9-sak feilet",
            message = upstreamDetail,
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            uri = URI.create("/fordel/fagsak/opprett/journalpost")
        )

        sendInnSøknad(sendSøknad, correlationId)
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectBody()
            .jsonPath("$.type").isEqualTo("/problem-details/innsending-feil")
            .jsonPath("$.title").isEqualTo("Feil ved innsending av søknad")
            .jsonPath("$.status").isEqualTo(500)
            .jsonPath("$.detail").isEqualTo("Det oppstod en serverfeil")
            .jsonPath("$.correlationId").isEqualTo(correlationId)
    }

    @Test
    fun `PSB send returnerer plain detail uendret ved ikke-JSON upstream-feil`(): Unit = runBlocking {
        val correlationId = UUID.randomUUID().toString()
        val norskIdent = "02020050122"
        val sendSøknad = opprettUtfyltSøknad(
            norskIdent = norskIdent,
            journalpostId = JournalpostIds.FerdigstiltMedSaksnummer
        )

        val upstreamDetail = "Opprettelse av fagsak feilet i k9-sak"

        coEvery { k9SakService.hentPerioderSomFinnesIK9ForSaksnummer(any()) } returns Pair(emptyList(), null)
        coEvery { k9SakService.opprettSakOgSendInnSøknad(any(), any(), any(), any(), any(), any()) } throws RestKallException(
            titel = "Restkall mot k9-sak feilet",
            message = upstreamDetail,
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            uri = URI.create("/fordel/fagsak/opprett/journalpost")
        )

        sendInnSøknad(sendSøknad, correlationId)
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectBody()
            .jsonPath("$.type").isEqualTo("/problem-details/innsending-feil")
            .jsonPath("$.title").isEqualTo("Feil ved innsending av søknad")
            .jsonPath("$.status").isEqualTo(500)
            .jsonPath("$.detail").isEqualTo(upstreamDetail)
            .jsonPath("$.correlationId").isEqualTo(correlationId)
    }

    @Test
    fun `PSB send beholder upstream status og problemdetail for ikke-500 feil`(): Unit = runBlocking {
        val correlationId = UUID.randomUUID().toString()
        val norskIdent = "02020050123"
        val sendSøknad = opprettUtfyltSøknad(
            norskIdent = norskIdent,
            journalpostId = JournalpostIds.FerdigstiltMedSaksnummer
        )

        val upstreamDetail = "Konflikt i k9-sak"

        coEvery { k9SakService.hentPerioderSomFinnesIK9ForSaksnummer(any()) } returns Pair(emptyList(), null)
        coEvery { k9SakService.opprettSakOgSendInnSøknad(any(), any(), any(), any(), any(), any()) } throws RestKallException(
            titel = "Restkall mot k9-sak feilet",
            message = upstreamDetail,
            httpStatus = HttpStatus.CONFLICT,
            uri = URI.create("/fordel/fagsak/opprett/journalpost")
        )

        sendInnSøknad(sendSøknad, correlationId)
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody()
            .jsonPath("$.type").isEqualTo("/problem-details/restkall-feil")
            .jsonPath("$.title").isEqualTo("Restkall mot k9-sak feilet")
            .jsonPath("$.status").isEqualTo(409)
            .jsonPath("$.detail").isEqualTo(upstreamDetail)
            .jsonPath("$.correlationId").isEqualTo(correlationId)
    }

    private suspend fun opprettUtfyltSøknad(
        norskIdent: String,
        journalpostId: String,
    ): SendSøknad {
        val søknadFraFrontend: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        søknadFraFrontend.replace("soekerId", norskIdent)
        søknadFraFrontend.replace("journalposter", arrayOf(journalpostId))

        val location = opprettNySøknad(opprettSøknad(norskIdent, journalpostId))
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody()
            .returnResult().responseHeaders.location!!

        val søknadId = location.path.substringAfterLast("/")
        søknadFraFrontend.replace("soeknadId", søknadId)

        oppdaterSøknad(søknadFraFrontend)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)

        return SendSøknad(
            norskIdent = norskIdent,
            soeknadId = søknadId
        )
    }

    private fun opprettNySøknad(opprettNySøknad: OpprettNySøknad) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .bodyValue(opprettNySøknad)
        .exchange()

    private fun oppdaterSøknad(søknadFraFrontend: SøknadJson) = webTestClient.put()
        .uri { it.path("/$api/$søknadTypeUri/oppdater").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .bodyValue(søknadFraFrontend)
        .exchange()

    private fun sendInnSøknad(sendSøknad: SendSøknad, correlationId: String) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri/send").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .header(CALL_ID_KEY, correlationId)
        .body(BodyInserters.fromValue(sendSøknad))
        .exchange()

    private fun opprettSøknad(
        personnummer: String,
        journalpostId: String,
        k9saksnummer: String = "ABC123",
    ): OpprettNySøknad {
        return OpprettNySøknad(
            norskIdent = personnummer,
            journalpostId = journalpostId,
            k9saksnummer = k9saksnummer,
            pleietrengendeIdent = null,
            annenPart = null
        )
    }
}
