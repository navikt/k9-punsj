package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.openapi.OasSoknadsfeil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.util.TestUtils.hentSøknadId
import no.nav.k9punsj.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class OmsorgspengerKroniskSyktBarnRoutesTest : AbstractContainerBaseTest() {

    private val api = "api"
    private val søknadTypeUri = "omsorgspenger-kronisk-sykt-barn-soknad"

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
            .expectBody()
            .jsonPath("$.søknader").isEmpty
    }

    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val pleietrengendeIdent = "02020050163"
        val opprettNySøknad = opprettSøknad(norskIdent, pleietrengendeIdent, UUID.randomUUID().toString())

        opprettNySøknad(opprettNySøknad)
    }


    @Test
    fun `Hente eksisterende mappe på person`(): Unit = runBlocking {
        val norskIdent = "02020050163"
        val pleietrengendeIdent = "01010050053"
        val journalpostId = UUID.randomUUID().toString()
        val opprettNySøknad = opprettSøknad(norskIdent, pleietrengendeIdent, journalpostId)

        opprettNySøknad(opprettNySøknad)
        hentMappe(norskIdent)
            .expectStatus().isOk
            .expectBody(SvarOmsKSBDto::class.java)
            .consumeWith {
                val journalposterDto = it.responseBody?.søknader?.first()?.journalposter
                Assertions.assertEquals(journalpostId, journalposterDto?.first())
            }
    }

    @Test
    fun `Hent en søknad`(): Unit = runBlocking {
        val søknad = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val norskIdent = "02030050163"
        val pleietrengendeIdent = "01010050053"
        val journalpostid = abs(Random(2224).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknad, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, pleietrengendeIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)

        hentMappe(hentSøknadId(location)!!, "")
            .expectStatus().isOk
            .expectBody(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
            .consumeWith {
                val faktiskJournalpostId = it.responseBody?.journalposter?.first()
                Assertions.assertEquals(journalpostid, faktiskJournalpostId)
            }
    }

    @Test
    fun `Oppdaterer en søknad`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val norskIdent = "02030050163"
        val pleietrengendeIdent = "01010050053"
        val journalpostid = abs(Random(1234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, pleietrengendeIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)

        leggerPåNySøknadId(søknadFraFrontend, location)

        oppdaterSøknad(norskIdent, søknadFraFrontend)
            .expectStatus().isOk
            .expectBody(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                Assertions.assertEquals(norskIdent, it.responseBody!!.soekerId)
            }
    }

    @Test
    fun `Oppdaterer en søknad med metadata`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val norskIdent = "02030050163"
        val pleietrengendeIdent = "01010050053"
        val journalpostid = abs(Random(1234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, pleietrengendeIdent, journalpostid)

        val location = opprettNySøknad(opprettNySøknad)

        leggerPåNySøknadId(søknadFraFrontend, location)

        val omsorgspengerKroniskSyktBarnSøknadDto = oppdaterSøknad(norskIdent, søknadFraFrontend)
            .expectStatus().isOk
            .expectBody(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(omsorgspengerKroniskSyktBarnSøknadDto)
        Assertions.assertEquals(norskIdent, omsorgspengerKroniskSyktBarnSøknadDto.soekerId)

        hentMappe(omsorgspengerKroniskSyktBarnSøknadDto.soeknadId, norskIdent)
            .expectStatus().isOk
            .expectBody(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                assertThat(omsorgspengerKroniskSyktBarnSøknadDto.metadata).isEqualTo(it.responseBody!!.metadata)
            }
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal verifisere at søknad er ok`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val pleietrengendeIdent = "01010050053"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, pleietrengendeIdent, journalpostid)

        validerSøknad(soeknad)
            .expectStatus().isEqualTo(HttpStatus.ACCEPTED)
            .expectBody(OasSoknadsfeil::class.java)
            .consumeWith {
                assertThat(it.responseBody?.feil).isNull()
            }
    }

    @Test
    fun `Skal kunne lagre flagg om medisinske og punsjet`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val pleietrengendeIdent = "01010050053"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, pleietrengendeIdent)

        hentMappe(oppdatertSoeknadDto.soeknadId, norskIdent)
            .expectStatus().isOk
            .expectBody(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                assertThat(it.responseBody!!.harInfoSomIkkeKanPunsjes).isEqualTo(true)
                assertThat(it.responseBody!!.harMedisinskeOpplysninger).isEqualTo(true)
            }
    }

    @Test
    fun `Innsending uten periode`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknadJson: SøknadJson = objectMapper().readValue(
            """
            {
              "soeknadId": "988bedeb-3324-4d2c-9277-dcbb5cc26577",
              "soekerId": "11111111111",
              "pleietrengendeIdent": "22222222222",
              "journalposter": [
                "123456"
              ],
              "mottattDato": "2023-12-07",
              "klokkeslett": "12:00",
              "barn": {
                "norskIdent": "22222222222",
                "foedselsdato": ""
              },
              "harInfoSomIkkeKanPunsjes": false,
              "harMedisinskeOpplysninger": true
            }
           """
        )
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknadJson, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknadJson, ident = norskIdent, journalpostid)

        validerSøknad(soeknadJson)
            .expectStatus().isEqualTo(HttpStatus.ACCEPTED)
            .expectBody(OasSoknadsfeil::class.java)
            .consumeWith {
                assertThat(it.responseBody?.feil).isNull()
            }

        val sendSøknad = lagSendSøknad(norskIdent = norskIdent, søknadId = soeknadJson["soeknadId"] as String)

        sendInnSøknad(sendSøknad)
    }

    @Test
    fun `skal få feil hvis barn ikke er fylt ut`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknad: SøknadJson = LesFraFilUtil.søknadUtenBarnFraFrontendOmsKSB()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        val body = validerSøknad(soeknad)
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody(OasSoknadsfeil::class.java)
            .consumeWith {
                val oasSoknadsfeil = it.responseBody
                Assertions.assertNotNull(oasSoknadsfeil)
                assertThat(oasSoknadsfeil!!.feil).isNotNull
                assertThat(oasSoknadsfeil.feil?.size).isEqualTo(1)
                assertThat(oasSoknadsfeil.feil?.get(0)?.felt).isEqualTo("ytelse.barn")
            }
    }

    private fun opprettSøknad(personnummer: String, pleietrengendeIdent: String, journalpostId: String) =
        OpprettNySøknad(
            norskIdent = personnummer,
            journalpostId = journalpostId,
            pleietrengendeIdent = pleietrengendeIdent,
            annenPart = null
        )

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
        pleietrengendeIdent: String,
        journalpostid: String = IdGenerator.nesteId(),
    ) {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, pleietrengendeIdent, journalpostid)

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = oppdaterSøknad(ident, soeknadJson)
            .expectStatus().isOk
            .expectBody(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        sendInnSøknad(sendSøknad)
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        pleietrengendeIdent: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): OmsorgspengerKroniskSyktBarnSøknadDto {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, pleietrengendeIdent, journalpostid)

        // oppretter en søknad
        val location = opprettNySøknad(innsendingForOpprettelseAvMappe)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = oppdaterSøknad(ident, soeknadJson)
            .expectStatus().isOk
            .expectBody(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
            .returnResult().responseBody!!

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)
        return søknadDtoFyltUt
    }

    private fun hentMappe(norskIdent: String) = webTestClient.get()
        .uri { it.path("/$api/$søknadTypeUri/mappe").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .exchange()

    private fun hentMappe(søknadId: String, norskIdent: String) = webTestClient.get()
        .uri { it.path("/$api/$søknadTypeUri/mappe/$søknadId").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .exchange()

    private fun opprettNySøknad(opprettNySøknad: OpprettNySøknad): URI = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .body(BodyInserters.fromValue(opprettNySøknad))
        .exchange()
        .expectStatus().isCreated
        .expectHeader().exists("Location")
        .returnResult(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)
        .responseHeaders.location!!

    private fun oppdaterSøknad(
        norskIdent: String,
        søknadFraFrontend: SøknadJson,
    ) = webTestClient.put()
        .uri { it.path("/$api/$søknadTypeUri/oppdater").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .header("X-Nav-NorskIdent", norskIdent)
        .body(BodyInserters.fromValue(søknadFraFrontend))
        .exchange()

    private fun validerSøknad(soeknad: SøknadJson) = webTestClient.post()
        .uri { it.path("/$api/$søknadTypeUri/valider").build() }
        .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
        .body(BodyInserters.fromValue(soeknad))
        .exchange()

    private fun sendInnSøknad(sendSøknad: SendSøknad) {
        webTestClient.post()
            .uri { it.path("/$api/$søknadTypeUri/send").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(sendSøknad))
            .exchange()
            .expectStatus().isAccepted
            .expectBody(Søknad::class.java)
    }
}
