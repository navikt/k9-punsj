package no.nav.k9punsj.omsorgspengeraleneomsorg

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

internal class OmsorgspengerAleneOmsorgRoutesTest : AbstractContainerBaseTest() {
    private val api = "api"
    private val søknadTypeUri = "omsorgspenger-alene-om-omsorgen-soknad"

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
        val pleietrengende = "01010050023"
        val opprettNySøknad = opprettSøknad(norskIdent, UUID.randomUUID().toString(), pleietrengende)

       opprettNySøknad(opprettNySøknad)
    }

    @Test
    fun `Skal verifisere at søknad er ok`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val pleietrengende = "01010050023"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsAO()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid, pleietrengende)

        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri/valider").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(soeknad))
            .exchange()
            .expectStatus().isAccepted
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val pleietrengende = "01010050023"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsAO()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid, pleietrengende)
        org.assertj.core.api.Assertions.assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal sjekke mapping av felter`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val pleietrengende = "01010050023"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsAO()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        val søknad =
            opprettOgLagreSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid, pleietrengende)

        webTestClient.get()
            .uri { it.path("/$api/$søknadTypeUri/mappe/${søknad.soeknadId}").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.begrunnelseForInnsending").isEqualTo("JEG VET IKKE")
            .jsonPath("$.barn.foedselsdato").isEqualTo("2018-10-30")
    }

    @Test
    fun `Oppdatere en søknad med metadata`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val pleietrengende = "01010050023"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsAO()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(
            soeknadJson = soeknad,
            ident = norskIdent,
            journalpostid = journalpostid,
            pleietrengende = pleietrengende
        )

        val body = oppdaterSøknad(soeknad)

        Assertions.assertNotNull(body)
        Assertions.assertEquals(norskIdent, body!!.soekerId)

        webTestClient.get()
            .uri { it.path("/$api/$søknadTypeUri/mappe/${soeknad["soeknadId"]}").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .header("X-Nav-NorskIdent", norskIdent)
            .exchange()
            .expectStatus().isOk
            .expectBody(OmsorgspengerAleneOmsorgSøknadDto::class.java)
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                Assertions.assertEquals(body.metadata, it.responseBody!!.metadata)
            }
    }

    private fun opprettSøknad(
        personnummer: String,
        journalpostId: String,
        pleietrengende: String,
    ): OpprettNySøknad {
        return OpprettNySøknad(
            norskIdent = personnummer,
            journalpostId = journalpostId,
            pleietrengendeIdent = pleietrengende,
            annenPart = null,
            k9saksnummer = null
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
    ): OmsorgspengerAleneOmsorgSøknadDto {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid, pleietrengende)

        // oppretter en søknad
        val location = webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
            .exchange()
            .expectStatus().isCreated
            .expectHeader().exists("Location")
            .returnResult(OmsorgspengerAleneOmsorgSøknadDto::class.java)
            .responseHeaders.location

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val omsorgspengerAleneOmsorgSøknadDto = oppdaterSøknad(soeknadJson)

        Assertions.assertNotNull(omsorgspengerAleneOmsorgSøknadDto)
        Assertions.assertNotNull(omsorgspengerAleneOmsorgSøknadDto!!.soekerId)

        return omsorgspengerAleneOmsorgSøknadDto
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

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = oppdaterSøknad(soeknadJson)

        Assertions.assertNotNull(søknadDtoFyltUt)
        Assertions.assertNotNull(søknadDtoFyltUt!!.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)
        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        org.assertj.core.api.Assertions.assertThat(kanSendeInn).isTrue

        // sender en søknad
        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri/send").build() }
            .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
            .body(BodyInserters.fromValue(sendSøknad))
            .exchange()
            .expectStatus().isAccepted
    }

    private fun opprettNySøknad(innsendingForOpprettelseAvMappe: OpprettNySøknad) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri").build() }
        .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
        .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
        .exchange()
        .expectStatus().isCreated
        .expectHeader().exists("Location")
        .returnResult(OmsorgspengerAleneOmsorgSøknadDto::class.java)
        .responseHeaders.location

    private fun oppdaterSøknad(soeknadJson: SøknadJson) = webTestClient.put()
        .uri { it.path("/$api/$søknadTypeUri/oppdater").build() }
        .header("Authorization", "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}")
        .body(BodyInserters.fromValue(soeknadJson))
        .exchange()
        .expectStatus().isOk
        .expectBody(OmsorgspengerAleneOmsorgSøknadDto::class.java)
        .returnResult().responseBody

    private fun lagSendSøknad(
        norskIdent: String,
        søknadId: String,
    ): SendSøknad {
        return SendSøknad(norskIdent, søknadId)
    }
}
