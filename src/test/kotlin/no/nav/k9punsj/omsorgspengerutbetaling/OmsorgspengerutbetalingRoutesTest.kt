package no.nav.k9punsj.omsorgspengerutbetaling

import kotlinx.coroutines.runBlocking
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.time.LocalDate
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class OmsorgspengerutbetalingRoutesTest : AbstractContainerBaseTest() {

    private val api = "api"
    private val søknadTypeUri = "omsorgspengerutbetaling-soknad"

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    @AfterEach
    internal fun tearDown() {
        cleanUpDB()
    }

    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`(): Unit = runBlocking {
        val norskIdent = "01110050053"

        hentMappe(norskIdent)
            .expectStatus().isOk
            .expectBody(SvarOmsUtDto::class.java)
            .consumeWith { assertThat(it.responseBody!!.søknader).isEmpty() }
    }

    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val opprettNySøknad = opprettSøknad(norskIdent, UUID.randomUUID().toString())

        opprettNySøknad(opprettNySøknad).expectStatus().isCreated
    }

    @Test
    fun `Hente eksisterende mappe på person`(): Unit = runBlocking {
        val norskIdent = "02020050163"
        val journalpostId = UUID.randomUUID().toString()
        val opprettNySøknad = opprettSøknad(norskIdent, journalpostId)

        opprettNySøknad(opprettNySøknad).expectStatus().isCreated

        val body = hentMappe(norskIdent)
            .expectStatus().isOk
            .expectBody(SvarOmsUtDto::class.java)
            .returnResult().responseBody!!

        val journalposterDto = body.søknader?.first()?.journalposter
        Assertions.assertEquals(journalpostId, journalposterDto?.first())
    }

    @Test
    fun `Hent en søknad`(): Unit = runBlocking {
        val søknad = LesFraFilUtil.søknadFraFrontend()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(2224).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknad, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad).expectStatus().isCreated
            .expectHeader().exists("Location")
            .returnResult(IdentOgJournalpost::class.java)
            .responseHeaders.location!!

        hentMappeGittSøknadId(location, norskIdent)
            .expectStatus().isOk
            .expectBody(OmsorgspengerutbetalingSøknadDto::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.soekerId).isEqualTo(norskIdent)
                assertThat(it.responseBody!!.journalposter).containsExactly(journalpostid)
            }
    }

    @Test
    fun `Oppdaterer en søknad`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOmsUt()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(1234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .returnResult(IdentOgJournalpost::class.java)
            .responseHeaders.location!!

        leggerPåNySøknadId(søknadFraFrontend, location)

        oppdaterSøknad(søknadFraFrontend)
            .expectStatus().isOk
            .expectBody(OmsorgspengerutbetalingSøknadDto::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.soekerId).isEqualTo(norskIdent)
            }
    }

    @Test
    fun `Oppdaterer en søknad med metadata`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOmsUt()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(1234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .returnResult(IdentOgJournalpost::class.java)
            .responseHeaders.location!!

        leggerPåNySøknadId(søknadFraFrontend, location)

        val body = oppdaterSøknad(søknadFraFrontend)
            .expectStatus().isOk
            .expectBody(OmsorgspengerutbetalingSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(body)
        Assertions.assertEquals(norskIdent, body.soekerId)

        val søknadViaGet = hentMappeGittSøknadId(location, norskIdent)
            .expectStatus().isOk
            .expectBody(OmsorgspengerutbetalingSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(søknadViaGet)
        assertThat(body.metadata).isEqualTo(søknadViaGet.metadata)
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsUt()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal fordele trekk av dager på enkelt dager slik at det validere ok`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsUtTrekk()
        val journalpostid = abs(Random(2234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)

        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal verifisere at søknad er ok`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsUt()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        validerSøknad(soeknad).expectStatus().isAccepted

    }

    @Test
    fun `Korrigering OMP UT med fraværsperioder fra tidiger år validerer riktigt år`(): Unit = runBlocking {
        // 03011939596 på OMS har två perioder i k9sak fra december 2022.
        // OmsUtKorrigering fjerner første perioden i 2022.
        val norskIdent = "03011939596"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsUtKorrigering()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        validerSøknad(soeknad).expectStatus().isAccepted
    }

    @Test
    fun `skal få feil hvis mottattDato ikke er fylt ut`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsUtFeil()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        validerSøknad(soeknad)
            .expectStatus().isBadRequest
            .expectBody(OasSoknadsfeil::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.feil?.get(0)?.feilkode).isEqualTo("mottattDato")
            }
    }

    @Test
    fun `Skal fordele trekk av dager på enkelt dager slik at det validere ok - kompleks versjon`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsUtTrekkKompleks()
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

        hentArbeidsForholdIderFraK9Sak(dtoSpørring)
            .expectStatus().isOk
            .expectBodyList(ArbeidsgiverMedArbeidsforholdId::class.java)
            .consumeWith<WebTestClient.ListBodySpec<ArbeidsgiverMedArbeidsforholdId>> {
                assertThat(it.responseBody!![0].arbeidsforholdId[0]).isEqualTo("randomArbeidsforholdId")
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
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .returnResult(IdentOgJournalpost::class.java)
            .responseHeaders.location!!

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = oppdaterSøknad(soeknadJson)
            .expectStatus().isOk
            .expectBody(OmsorgspengerutbetalingSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        webTestClient.post()
            .uri("/$api/$søknadTypeUri/send")
            .header("Authorization", saksbehandlerAuthorizationHeader)
            .bodyValue(sendSøknad)
            .exchange()
            .expectStatus().isAccepted
            .expectBody(Søknad::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.søknadId.id).isEqualTo(søknadId)
            }
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ) {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .returnResult(IdentOgJournalpost::class.java)
            .responseHeaders.location!!

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        oppdaterSøknad(soeknadJson)
            .expectStatus().isOk
            .expectBody(OmsorgspengerutbetalingSøknadDto::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.soekerId).isEqualTo(ident)
            }
    }

    private fun hentMappe(norskIdent: String) = webTestClient.get()
        .uri("/$api/$søknadTypeUri/mappe")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .exchange()

    private fun opprettNySøknad(opprettNySøknad: IdentOgJournalpost) = webTestClient.post()
        .uri("/$api/$søknadTypeUri")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .body(BodyInserters.fromValue(opprettNySøknad))
        .exchange()

    private fun hentMappeGittSøknadId(
        location: URI,
        norskIdent: String,
    ) = webTestClient.get()
        .uri("/$api/$søknadTypeUri/mappe/${hentSøknadId(location)}")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .exchange()

    private fun oppdaterSøknad(søknadFraFrontend: SøknadJson) = webTestClient.put()
        .uri("/$api/$søknadTypeUri/oppdater")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .bodyValue(søknadFraFrontend)
        .exchange()

    private fun validerSøknad(soeknad: SøknadJson) = webTestClient.post()
        .uri("/$api/$søknadTypeUri/valider")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .bodyValue(soeknad)
        .exchange()

    private fun hentArbeidsForholdIderFraK9Sak(dtoSpørring: MatchFagsakMedPeriode) = webTestClient.post()
        .uri("/$api/$søknadTypeUri/k9sak/arbeidsforholdIder")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .bodyValue(dtoSpørring)
        .exchange()
}
