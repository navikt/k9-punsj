package no.nav.k9punsj.korrigeringinntektsmelding

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.IdentOgJournalpost
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.MatchFagsakMedPeriode
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.openapi.OasSoknadsfeil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.util.TestUtils.hentSøknadId
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.time.LocalDate
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class KorrigeringInntektsmeldingDtoRoutesTest : AbstractContainerBaseTest() {

    private val api = "api"
    private val søknadTypeUri = "omsorgspenger-soknad"

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    @AfterEach
    internal fun tearDown() {
        cleanUpDB()
    }

    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`(): Unit = runBlocking {
        val norskIdent = "01110050053"
        webTestClient.get()
            .uri { it.path("/$api/$søknadTypeUri/mappe").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .header("X-Nav-NorskIdent", norskIdent)
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.søknader").isEmpty
    }

    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val opprettNySøknad = opprettSøknad(norskIdent, UUID.randomUUID().toString())

        opprettNySøknad(opprettNySøknad)
    }

    @Test
    fun `Hente eksisterende mappe på person`(): Unit = runBlocking {
        val norskIdent = "02020050163"
        val journalpostId = UUID.randomUUID().toString()
        val opprettNySøknad = opprettSøknad(norskIdent, journalpostId)

        opprettNySøknad(opprettNySøknad)

        webTestClient.get()
            .uri { it.path("/$api/$søknadTypeUri/mappe").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .header("X-Nav-NorskIdent", norskIdent)
            .exchange()
            .expectStatus().isOk
            .expectBody(SvarOmsDto::class.java)
            .consumeWith {
                val journalposter = it.responseBody?.søknader?.first()?.journalposter
                Assertions.assertEquals(journalpostId, journalposter?.first())
            }
    }

    @Test
    fun `Hent en søknad`(): Unit = runBlocking {
        val søknad = LesFraFilUtil.søknadFraFrontend()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(2224).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknad, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)

        webTestClient.get()
            .uri { it.path("/$api/$søknadTypeUri/mappe/${hentSøknadId(location)}").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .header("X-Nav-NorskIdent", norskIdent)
            .exchange()
            .expectStatus().isOk
            .expectBody(KorrigeringInntektsmeldingDto::class.java)
            .consumeWith {
                val journalposter = it.responseBody?.journalposter
                Assertions.assertEquals(journalpostid, journalposter?.first())
            }
    }

    @Test
    fun `Oppdaterer en søknad`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOms()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(1234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)

        leggerPåNySøknadId(søknadFraFrontend, location)

        webTestClient.put()
            .uri { it.path("/$api/$søknadTypeUri/oppdater").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(søknadFraFrontend))
            .exchange()
            .expectStatus().isOk
            .expectBody(KorrigeringInntektsmeldingDto::class.java)
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                Assertions.assertEquals(norskIdent, it.responseBody!!.soekerId)
            }
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOms()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal fordele trekk av dager på enkelt dager slik at det validere ok`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsTrekk()
        val journalpostid = abs(Random(2234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal verifisere at søknad er ok`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOms()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri/valider").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(soeknad))
            .exchange()
            .expectStatus().isAccepted
            .expectBody(OasSoknadsfeil::class.java)
            .consumeWith {
                Assertions.assertNull(it.responseBody?.feil)
            }
    }

    @Test
    fun `skal få feil hvis mottattDato ikke er fylt ut`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsFeil()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri/valider").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(soeknad))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody(OasSoknadsfeil::class.java)
            .consumeWith {
                Assertions.assertEquals("mottattDato", it.responseBody?.feil?.get(0)?.feilkode)
            }
    }

    @Test
    fun `Skal fordele trekk av dager på enkelt dager slik at det validere ok - kompleks versjon`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsTrekkKompleks()
        val journalpostid = abs(Random(2256234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal hente arbeidsforholdIder fra k9-sak`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val dtoSpørring =
            MatchFagsakMedPeriode(
                norskIdent,
                PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1))
            )

        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri/k9sak/arbeidsforholdIder").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(dtoSpørring))
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ArbeidsgiverMedArbeidsforholdId::class.java)
            .consumeWith<WebTestClient.ListBodySpec<ArbeidsgiverMedArbeidsforholdId>> {
                Assertions.assertEquals("randomArbeidsforholdId", it.responseBody?.first()?.arbeidsforholdId?.first())
            }
    }

    private fun opprettSøknad(
        personnummer: String,
        journalpostId: String,
    ): IdentOgJournalpost {
        return IdentOgJournalpost(personnummer, journalpostId)
    }

    private fun tilpasserSøknadsMalTilTesten(
        søknad: MutableMap<String, Any?>,
        norskIdent: String,
        journalpostId: String? = null,
    ) {
        søknad.replace("soekerId", norskIdent)
        if (journalpostId != null) søknad.replace("journalposter", arrayOf(journalpostId))
    }

    private fun leggerPåNySøknadId(søknadFraFrontend: MutableMap<String, Any?>, location: URI?) {
        val path = location?.path
        val søknadId = path?.substring(path.lastIndexOf('/'))
        val trim = søknadId?.trim('/')
        søknadFraFrontend.replace("soeknadId", trim)
    }

    private fun lagSendSøknad(
        norskIdent: String,
        søknadId: String,
    ): SendSøknad {
        return SendSøknad(norskIdent, søknadId)
    }

    private suspend fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ) {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = webTestClient.put()
            .uri { it.path("/$api/$søknadTypeUri/oppdater").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(soeknadJson))
            .exchange()
            .expectStatus().isOk
            .expectBody(KorrigeringInntektsmeldingDto::class.java)
            .returnResult()
            .responseBody

        Assertions.assertNotNull(søknadDtoFyltUt)
        Assertions.assertNotNull(søknadDtoFyltUt!!.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri/send").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(sendSøknad))
            .exchange()
            .expectStatus().isAccepted
            .expectBody(Søknad::class.java)
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ) {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val responseBody = webTestClient.put()
            .uri { it.path("/$api/$søknadTypeUri/oppdater").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(soeknadJson))
            .exchange()
            .expectStatus().isOk
            .expectBody(KorrigeringInntektsmeldingDto::class.java)
            .returnResult()
            .responseBody

        Assertions.assertNotNull(responseBody)
        Assertions.assertNotNull(responseBody!!.soekerId)
    }

    private fun opprettNySøknad(requestBody: IdentOgJournalpost) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri").build() }
        .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
        .body(BodyInserters.fromValue(requestBody))
        .exchange()
        .expectStatus().isCreated
        .expectHeader().exists("Location")
        .returnResult<Any>()
        .responseHeaders.location
}
