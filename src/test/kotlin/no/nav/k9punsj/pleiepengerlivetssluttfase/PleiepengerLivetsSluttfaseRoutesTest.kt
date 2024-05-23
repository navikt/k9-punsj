package no.nav.k9punsj.pleiepengerlivetssluttfase

import kotlinx.coroutines.runBlocking
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class PleiepengerLivetsSluttfaseRoutesTest : AbstractContainerBaseTest() {

    private val api = "api"
    private val søknadTypeUri = "pleiepenger-livets-sluttfase-soknad"

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
            .expectBody(SvarPlsDto::class.java)
            .consumeWith {
                val body = it.responseBody!!
                assertThat(body.søknader!!).isEmpty()
            }
    }

    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val pleietrengende = "01010050023"
        val opprettNySøknad = opprettSøknad(norskIdent, UUID.randomUUID().toString(), pleietrengende)

        opprettNySøknad(opprettNySøknad).expectStatus().isCreated
    }

    @Test
    fun `Skal verifisere at søknad er ok`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val pleietrengende = "01010050023"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendPls()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid, pleietrengende)

        validerSøknad(soeknad).expectStatus().isAccepted
    }

    @Test
    fun `Oppdatere en søknad med metadata`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val pleietrengende = "01010050023"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendPls()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid, pleietrengende)

        val body = oppdaterSøknad(soeknad)
            .expectStatus().isOk
            .expectBody(PleiepengerLivetsSluttfaseSøknadDto::class.java)
            .returnResult().responseBody!!

        hentMappeGittSøknadId(soeknad, norskIdent)
            .expectStatus().isOk
            .expectBody(PleiepengerLivetsSluttfaseSøknadDto::class.java)
            .consumeWith {
                assertThat(body.metadata).isEqualTo(it.responseBody!!.metadata)
            }
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val pleietrengende = "01010050023"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendPls()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid, pleietrengende)
        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    private fun opprettSøknad(
        personnummer: String,
        journalpostId: String,
        pleietrengende: String,
    ): OpprettNySøknad {
        return OpprettNySøknad(
            personnummer,
            journalpostId,
            pleietrengende,
            null
        )
    }

    private fun tilpasserSøknadsMalTilTesten(
        søknad: MutableMap<String, Any?>,
        norskIdent: String,
        journalpostId: String? = null,
    ) {
        søknad.replace("soekerId", norskIdent)
        if (journalpostId != null) søknad.replace("journalposter", arrayOf(journalpostId))
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
        pleietrengende: String,
    ) {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid, pleietrengende)

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody(PleiepengerLivetsSluttfaseSøknadDto::class.java)
            .returnResult().responseHeaders.location

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        oppdaterSøknad(soeknadJson)
            .expectStatus().isOk
            .expectBody(PleiepengerLivetsSluttfaseSøknadDto::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.soekerId).isEqualTo(ident)
            }
    }

    private fun leggerPåNySøknadId(søknadFraFrontend: MutableMap<String, Any?>, location: URI?) {
        val path = location?.path
        val søknadId = path?.substring(path.lastIndexOf('/'))
        val trim = søknadId?.trim('/')
        søknadFraFrontend.replace("soeknadId", trim)
    }

    private suspend fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
        pleietrengende: String,
    ) {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid, pleietrengende)

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .expectBody(PleiepengerLivetsSluttfaseSøknadDto::class.java)
            .returnResult().responseHeaders.location

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = oppdaterSøknad(soeknadJson)
            .expectStatus().isOk
            .expectBody(PleiepengerLivetsSluttfaseSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        sendInnSøknad(sendSøknad)
            .expectStatus().isAccepted
            .expectBody(Søknad::class.java)
            .consumeWith {
                assertThat(it.responseBody!!.søknadId.id).isEqualTo(søknadId)
            }
    }

    private fun lagSendSøknad(
        norskIdent: String,
        søknadId: String,
    ): SendSøknad {
        return SendSøknad(norskIdent, søknadId)
    }

    private fun hentMappe(norskIdent: String) = webTestClient.get()
        .uri("/$api/$søknadTypeUri/mappe")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .exchange()

    private fun opprettNySøknad(opprettNySøknad: OpprettNySøknad) = webTestClient.post()
        .uri("/$api/$søknadTypeUri")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .body(BodyInserters.fromValue(opprettNySøknad))
        .exchange()

    private fun validerSøknad(soeknad: SøknadJson) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri/valider").build() }
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .bodyValue(soeknad)
        .exchange()

    private fun oppdaterSøknad(soeknad: SøknadJson) = webTestClient.put()
        .uri("/$api/$søknadTypeUri/oppdater")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .bodyValue(soeknad)
        .exchange()

    private fun hentMappeGittSøknadId(
        soeknad: SøknadJson,
        norskIdent: String,
    ) = webTestClient.get()
        .uri("/$api/$søknadTypeUri/mappe/${soeknad["soeknadId"]}")
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .exchange()

    private fun sendInnSøknad(sendSøknad: SendSøknad) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri/send").build() }
        .header("Authorization", saksbehandlerAuthorizationHeader)
        .bodyValue(sendSøknad)
        .exchange()
}
