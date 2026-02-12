package no.nav.k9punsj.pleiepengersyktbarn

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.CALL_ID_KEY
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.wiremock.JournalpostIds
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import java.util.UUID

internal class PleiepengerSyktBarnValiderErrorContractTest : AbstractContainerBaseTest() {

    @MockkBean
    private lateinit var k9SakService: K9SakService

    private val api = "api"
    private val søknadTypeUri = "pleiepenger-sykt-barn-soknad"

    @AfterEach
    fun teardown() {
        cleanUpDB()
    }

    @Test
    fun `PSB valider returnerer ProblemDetail med valideringsfeil`(): Unit = runBlocking {
        val correlationId = UUID.randomUUID().toString()
        val norskIdent = "02020050131"

        coEvery { k9SakService.hentPerioderSomFinnesIK9ForSaksnummer(any()) } returns Pair(emptyList(), null)

        val søknad = opprettUtfyltSøknad(
            søknadFraFrontend = LesFraFilUtil.tidSøknad(),
            norskIdent = norskIdent,
            journalpostId = JournalpostIds.FerdigstiltMedSaksnummer
        )

        validerSøknad(søknad, correlationId)
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.type").isEqualTo("/problem-details/validering-feil")
            .jsonPath("$.title").isEqualTo("Ugyldig søknad for validering")
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.detail").isEqualTo("Søknaden inneholder valideringsfeil")
            .jsonPath("$.soeknadId").isEqualTo(søknad["soeknadId"].toString())
            .jsonPath("$.feil").isArray
            .jsonPath("$.feil[0].feilkode").exists()
            .jsonPath("$.correlationId").isEqualTo(correlationId)
    }

    @Test
    fun `PSB valider returnerer ProblemDetail når søknad ikke finnes`(): Unit = runBlocking {
        val correlationId = UUID.randomUUID().toString()
        val søknadId = UUID.randomUUID().toString()
        val søknad = LesFraFilUtil.søknadFraFrontend().apply {
            put("soekerId", "02020050132")
            put("soeknadId", søknadId)
            put("journalposter", arrayOf(JournalpostIds.FerdigstiltMedSaksnummer))
        }

        validerSøknad(søknad, correlationId)
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.type").isEqualTo("/problem-details/validering-feil")
            .jsonPath("$.title").isEqualTo("Ugyldig søknad for validering")
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.detail").isEqualTo("Søknad med id $søknadId finnes ikke.")
            .jsonPath("$.correlationId").isEqualTo(correlationId)
    }

    @Test
    fun `PSB valider returnerer ProblemDetail ved uventet feil`(): Unit = runBlocking {
        val correlationId = UUID.randomUUID().toString()
        val norskIdent = "02020050133"
        val søknad = opprettUtfyltSøknad(
            søknadFraFrontend = LesFraFilUtil.søknadFraFrontend(),
            norskIdent = norskIdent,
            journalpostId = JournalpostIds.FerdigstiltMedSaksnummer
        )

        coEvery { k9SakService.hentPerioderSomFinnesIK9ForSaksnummer(any()) } throws RuntimeException("K9 sak utilgjengelig")

        validerSøknad(søknad, correlationId)
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.type").isEqualTo("/problem-details/validering-uventet-feil")
            .jsonPath("$.title").isEqualTo("Feil ved validering av søknad")
            .jsonPath("$.status").isEqualTo(500)
            .jsonPath("$.detail").isEqualTo("K9 sak utilgjengelig")
            .jsonPath("$.correlationId").isEqualTo(correlationId)
    }

    @Test
    fun `PSB valider returnerer 202 for gyldig søknad`(): Unit = runBlocking {
        val norskIdent = "02020050134"

        coEvery { k9SakService.hentPerioderSomFinnesIK9ForSaksnummer(any()) } returns Pair(emptyList(), null)

        val søknad = opprettUtfyltSøknad(
            søknadFraFrontend = LesFraFilUtil.søknadFraFrontend(),
            norskIdent = norskIdent,
            journalpostId = JournalpostIds.FerdigstiltMedSaksnummer
        )

        validerSøknad(søknad)
            .expectStatus().isAccepted
    }

    private suspend fun opprettUtfyltSøknad(
        søknadFraFrontend: SøknadJson,
        norskIdent: String,
        journalpostId: String,
    ): SøknadJson {
        søknadFraFrontend["soekerId"] = norskIdent
        søknadFraFrontend["journalposter"] = arrayOf(journalpostId)

        val location = opprettNySøknad(opprettSøknad(norskIdent, journalpostId))
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody()
            .returnResult().responseHeaders.location!!

        val søknadId = location.path.substringAfterLast("/")
        søknadFraFrontend["soeknadId"] = søknadId

        oppdaterSøknad(søknadFraFrontend)
            .expectStatus().isOk
            .expectBody(PleiepengerSyktBarnSøknadDto::class.java)

        return søknadFraFrontend
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

    private fun validerSøknad(søknadFraFrontend: SøknadJson, correlationId: String? = null) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri/valider").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .apply {
            if (!correlationId.isNullOrBlank()) {
                header(CALL_ID_KEY, correlationId)
            }
        }
        .bodyValue(søknadFraFrontend)
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
