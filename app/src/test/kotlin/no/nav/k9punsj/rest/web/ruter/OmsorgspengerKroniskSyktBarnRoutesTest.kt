package no.nav.k9punsj.rest.web.ruter

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.*
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.rest.web.OpprettNySøknad
import no.nav.k9punsj.rest.web.SendSøknad
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.*
import no.nav.k9punsj.rest.web.openapi.OasMatchfagsakMedPeriode
import no.nav.k9punsj.rest.web.openapi.OasSoknadsfeil
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.time.LocalDate
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

@ExtendWith(SpringExtension::class, MockKExtension::class)
class OmsorgspengerKroniskSyktBarnRoutesTest {

    private val client = TestSetup.client
    private val api = "api"
    private val søknadTypeUri = FagsakYtelseTypeUri.OMSORGSPENGER_KRONISK_SYKT_BARN
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    private val journalpostRepository = DatabaseUtil.getJournalpostRepo()

    @AfterEach
    internal fun tearDown() {
        DatabaseUtil.cleanDB()
    }

    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`(): Unit = runBlocking {
        val norskIdent = "01110050053"
        val (status, body) = client.get()
            // get omsorgspenger-kronisk-sykt-barn-soknad/mappe
            .uri { it.pathSegment(api, søknadTypeUri, "mappe").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitStatusWithBody<SvarOmsKSBDto>()
        Assertions.assertEquals(HttpStatus.OK, status)
        Assertions.assertTrue(body.søknader!!.isEmpty())
    }

    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val opprettNySøknad = opprettSøknad(norskIdent, UUID.randomUUID().toString())
        val httpStatus = client.post()
            // post omsorgspenger-kronisk-sykt-barn-soknad/mappe
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitStatuscode()
        Assertions.assertEquals(HttpStatus.CREATED, httpStatus)
    }

    @Test
    fun `Hente eksisterende mappe på person`(): Unit = runBlocking {
        val norskIdent = "02020050163"
        val journalpostId = UUID.randomUUID().toString()
        val opprettNySøknad = opprettSøknad(norskIdent, journalpostId)

        val httpStatus = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitStatuscode()
        Assertions.assertEquals(HttpStatus.CREATED, httpStatus)

        val (status, body) = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitStatusWithBody<SvarOmsKSBDto>()
        Assertions.assertEquals(HttpStatus.OK, status)

        val journalposterDto = body.søknader?.first()?.journalposter
        Assertions.assertEquals(journalpostId, journalposterDto?.first())
    }

    @Test
    fun `Hent en søknad`(): Unit = runBlocking {
        val søknad = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(2224).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknad, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitExchangeBlocking()

        val location = resPost.headers().asHttpHeaders().location
        Assertions.assertEquals(HttpStatus.CREATED, resPost.statusCode())
        Assertions.assertNotNull(location)

        val søknadViaGet = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", hentSøknadId(location)).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitBodyWithType<OmsorgspengerKroniskSyktBarnSøknadDto>()

        Assertions.assertNotNull(søknadViaGet)
        Assertions.assertEquals(journalpostid, søknadViaGet.journalposter?.first())
    }

    @Test
    fun `Oppdaterer en søknad`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(1234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitExchangeBlocking()

        val location = resPost.headers().asHttpHeaders().location
        Assertions.assertEquals(HttpStatus.CREATED, resPost.statusCode())
        Assertions.assertNotNull(location)

        leggerPåNySøknadId(søknadFraFrontend, location)

        val (status, body) = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(søknadFraFrontend))
            .awaitStatusWithBody<OmsorgspengerKroniskSyktBarnSøknadDto>()

        Assertions.assertNotNull(body)
        Assertions.assertEquals(norskIdent, body.soekerId)
        Assertions.assertEquals(HttpStatus.OK, status)
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        val (_, status, body) = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        assertThat(body.feil).isNull()
        Assertions.assertEquals(HttpStatus.ACCEPTED, status)
        assertThat(journalpostRepository.kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal verifisere at søknad er ok`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        val (status, body) = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "valider").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknad))
            .awaitStatusWithBody<OasSoknadsfeil>()

        assertThat(body.feil).isNull()

        Assertions.assertEquals(HttpStatus.ACCEPTED, status)
    }

    @Test
    @Disabled("Test passer ikke fordi k9-format ikke validerer noe på denne ytelsen.")
    fun `skal få feil hvis barn ikke er fylt ut`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknad: SøknadJson = LesFraFilUtil.søknadUtenBarnFraFrontendOmsKSB()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        val (status, body) = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "valider").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknad))
            .awaitStatusWithBody<OasSoknadsfeil>()

        assertThat(body.feil?.get(0)?.felt).isEqualTo("ytelse.barn")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, status)
    }

    @Test
    fun `Skal hente arbeidsforholdIder fra k9-sak`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val dtoSpørring =
            OasMatchfagsakMedPeriode(norskIdent, PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1)))

        val oppdatertSoeknadDto = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "k9sak", "arbeidsforholdIder").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(dtoSpørring))
            .awaitBodyWithType<List<ArbeidsgiverMedArbeidsforholdId>>()

        Assertions.assertEquals("randomArbeidsforholdId", oppdatertSoeknadDto[0].arbeidsforholdId[0])
    }

    private fun opprettSøknad(personnummer: NorskIdentDto, journalpostId: String) = OpprettNySøknad(
        norskIdent = personnummer,
        journalpostId = journalpostId,
        barnIdent = null
    )

    private fun tilpasserSøknadsMalTilTesten(
        søknad: MutableMap<String, Any?>,
        norskIdent: String,
        journalpostId: String? = null,
    ) {
        søknad.replace("soekerId", norskIdent)
        if (journalpostId != null) søknad.replace("journalposter", arrayOf(journalpostId))
    }

    private fun hentSøknadId(location: URI?): String? {
        val path = location?.path
        val søknadId = path?.substring(path.lastIndexOf('/'))
        return søknadId?.trim('/')
    }

    private fun leggerPåNySøknadId(søknadFraFrontend: MutableMap<String, Any?>, location: URI?) {
        val path = location?.path
        val søknadId = path?.substring(path.lastIndexOf('/'))
        val trim = søknadId?.trim('/')
        søknadFraFrontend.replace("soeknadId", trim)
    }

    private fun lagSendSøknad(
        norskIdent: NorskIdentDto,
        søknadId: SøknadIdDto,
    ): SendSøknad {
        return SendSøknad(norskIdent, søknadId)
    }

    private suspend fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): Triple<SøknadIdDto, HttpStatus, OasSoknadsfeil> {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
            .awaitExchangeBlocking()

        val location = resPost.headers().asHttpHeaders().location
        Assertions.assertEquals(HttpStatus.CREATED, resPost.statusCode())
        Assertions.assertNotNull(location)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt: OmsorgspengerKroniskSyktBarnSøknadDto = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknadJson))
            .awaitBodyWithType()

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        val (httpStatus, oasSoknadsfeil) = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "send").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(sendSøknad))
            .awaitStatusWithBody<OasSoknadsfeil>()

        return Triple(søknadId, httpStatus, oasSoknadsfeil)
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): OmsorgspengerKroniskSyktBarnSøknadDto {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
            .awaitExchangeBlocking()

        val location = resPost.headers().asHttpHeaders().location
        Assertions.assertEquals(HttpStatus.CREATED, resPost.statusCode())
        Assertions.assertNotNull(location)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknadJson))
            .awaitBodyWithType<OmsorgspengerKroniskSyktBarnSøknadDto>()

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)
        return søknadDtoFyltUt
    }
}
