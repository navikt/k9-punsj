package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.k9punsj.openapi.OasSoknadsfeil
import no.nav.k9punsj.util.DbContainerInitializer
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.SøknadJson
import no.nav.k9punsj.util.TestUtils.hentSøknadId
import no.nav.k9punsj.util.WebClientUtils.awaitStatusWithBody
import no.nav.k9punsj.util.WebClientUtils.getAndAssert
import no.nav.k9punsj.util.WebClientUtils.postAndAssert
import no.nav.k9punsj.util.WebClientUtils.postAndAssertAwaitWithStatusAndBody
import no.nav.k9punsj.util.WebClientUtils.putAndAssert
import no.nav.k9punsj.utils.objectMapper
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
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

/*
@ExtendWith(SpringExtension::class, MockKExtension::class)
class OmsorgspengerKroniskSyktBarnRoutesTest {

    private val client = TestSetup.client
    private val api = "api"
    private val søknadTypeUri = "omsorgspenger-kronisk-sykt-barn-soknad"
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    private val journalpostRepository = DbContainerInitializer.getJournalpostRepo()

    @AfterEach
    internal fun tearDown() {
        DbContainerInitializer.cleanDB()
    }

    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`(): Unit = runBlocking {
        val responsebody = client.getAndAssert<SvarOmsKSBDto>(
            norskIdent = "01110050053",
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            api,
            søknadTypeUri,
            "mappe"
        )
        Assertions.assertTrue(responsebody.søknader!!.isEmpty())
    }

    @Test
    fun `Opprette ny mappe på person`(): Unit = runBlocking {
        val norskIdent = "01010050053"
        val opprettNySøknad = opprettSøknad(norskIdent, UUID.randomUUID().toString())

        client.postAndAssert<OpprettNySøknad>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(opprettNySøknad),
            api,
            søknadTypeUri
        )
    }

    @Test
    fun `Hente eksisterende mappe på person`(): Unit = runBlocking {
        val norskIdent = "02020050163"
        val journalpostId = UUID.randomUUID().toString()
        val opprettNySøknad = opprettSøknad(norskIdent, journalpostId)

        client.postAndAssert<OpprettNySøknad>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(opprettNySøknad),
            api,
            søknadTypeUri
        )

        val responsebody = client.getAndAssert<SvarOmsKSBDto>(
            norskIdent = norskIdent,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            api,
            søknadTypeUri,
            "mappe"
        )

        val journalposterDto = responsebody.søknader?.first()?.journalposter
        Assertions.assertEquals(journalpostId, journalposterDto?.first())
    }

    @Test
    fun `Hent en søknad`(): Unit = runBlocking {
        val søknad = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(2224).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknad, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val resPost = client.postAndAssert<OpprettNySøknad>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(opprettNySøknad),
            api,
            søknadTypeUri
        )

        val location = resPost.headers().asHttpHeaders().location
        Assertions.assertNotNull(location)

        val res = client.getAndAssert<OmsorgspengerKroniskSyktBarnSøknadDto>(
            norskIdent = "",
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            api,
            søknadTypeUri,
            "mappe",
            hentSøknadId(location)!!
        )

        Assertions.assertNotNull(res)
        Assertions.assertEquals(journalpostid, res.journalposter?.first())
    }

    @Test
    fun `Oppdaterer en søknad`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(1234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val nySøknadRespons = client.postAndAssert<OpprettNySøknad>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(opprettNySøknad),
            api,
            søknadTypeUri
        )

        val location = nySøknadRespons.headers().asHttpHeaders().location
        Assertions.assertNotNull(location)

        leggerPåNySøknadId(søknadFraFrontend, location)

        val body = client.putAndAssert<MutableMap<String, Any?>, OmsorgspengerKroniskSyktBarnSøknadDto>(
            norskIdent = norskIdent,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            requestBody = BodyInserters.fromValue(søknadFraFrontend),
            api,
            søknadTypeUri,
            "oppdater"
        )

        Assertions.assertNotNull(body)
        Assertions.assertEquals(norskIdent, body.soekerId)
    }

    @Test
    fun `Oppdaterer en søknad med metadata`(): Unit = runBlocking {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val norskIdent = "02030050163"
        val journalpostid = abs(Random(1234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val nySøknadRespons = client.postAndAssert<OpprettNySøknad>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(opprettNySøknad),
            api,
            søknadTypeUri
        )

        val location = nySøknadRespons.headers().asHttpHeaders().location
        Assertions.assertNotNull(location)

        leggerPåNySøknadId(søknadFraFrontend, location)

        val body = client.putAndAssert<MutableMap<String, Any?>, OmsorgspengerKroniskSyktBarnSøknadDto>(
            norskIdent = norskIdent,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            requestBody = BodyInserters.fromValue(søknadFraFrontend),
            api,
            søknadTypeUri,
            "oppdater"
        )

        Assertions.assertNotNull(body)
        Assertions.assertEquals(norskIdent, body.soekerId)

        val søknadViaGet = client.getAndAssert<OmsorgspengerKroniskSyktBarnSøknadDto>(
            norskIdent = norskIdent,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            api,
            søknadTypeUri,
            "mappe",
            hentSøknadId(location)!!
        )

        Assertions.assertNotNull(søknadViaGet)
        assertThat(body.metadata).isEqualTo(søknadViaGet.metadata)
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`(): Unit = runBlocking {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        val journalpostid = abs(Random(56234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        val body = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        assertThat(body.feil).isNull()
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
    fun `Skal kunne lagre flagg om medisinske og punsjet`(): Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsKSB()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val søknad = client.getAndAssert<OmsorgspengerKroniskSyktBarnSøknadDto>(
            norskIdent = null,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            api,
            søknadTypeUri,
            "mappe",
            oppdatertSoeknadDto.soeknadId
        )

        Assertions.assertNotNull(søknad)
        assertThat(søknad.harInfoSomIkkeKanPunsjes).isEqualTo(true)
        assertThat(søknad.harMedisinskeOpplysninger).isEqualTo(true)
    }

    @Test
    fun `Innsending uten periode`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknadJson: SøknadJson = objectMapper().readValue("""
            {
              "soeknadId": "988bedeb-3324-4d2c-9277-dcbb5cc26577",
              "soekerId": "11111111111",
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

        val body = client.postAndAssertAwaitWithStatusAndBody<SøknadJson, OasSoknadsfeil>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            navNorskIdentHeader = null,
            assertStatus = HttpStatus.ACCEPTED,
            requestBody = BodyInserters.fromValue(soeknadJson),
            api,
            søknadTypeUri,
            "valider"
        )

        assertThat(body.feil?.isEmpty())

        val sendSøknad = lagSendSøknad(norskIdent = norskIdent, søknadId = soeknadJson["soeknadId"] as String)

        val resultat = client.postAndAssertAwaitWithStatusAndBody<SendSøknad, OasSoknadsfeil>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            navNorskIdentHeader = null,
            assertStatus = HttpStatus.ACCEPTED,
            requestBody = BodyInserters.fromValue(sendSøknad),
            api,
            søknadTypeUri,
            "send"
        )

        assertThat(resultat.feil?.isEmpty())
    }

    @Test
    @Disabled("Test passer ikke fordi k9-format ikke validerer noe på denne ytelsen.")
    fun `skal få feil hvis barn ikke er fylt ut`(): Unit = runBlocking {
        val norskIdent = "02022352122"
        val soeknad: SøknadJson = LesFraFilUtil.søknadUtenBarnFraFrontendOmsKSB()
        val journalpostid = abs(Random(234234).nextInt()).toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        val body = client.postAndAssertAwaitWithStatusAndBody<SøknadJson, OasSoknadsfeil>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            navNorskIdentHeader = null,
            assertStatus = HttpStatus.BAD_REQUEST,
            requestBody = BodyInserters.fromValue(soeknad),
            api,
            søknadTypeUri,
            "valider"
        )

        assertThat(body.feil?.get(0)?.felt).isEqualTo("ytelse.barn")
    }

    private fun opprettSøknad(personnummer: String, journalpostId: String) =
        OpprettNySøknad(
            norskIdent = personnummer,
            journalpostId = journalpostId,
            pleietrengendeIdent = null,
            annenPart = null
        )

    private fun tilpasserSøknadsMalTilTesten(
        søknad: MutableMap<String, Any?>,
        norskIdent: String,
        journalpostId: String? = null
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
        søknadId: String
    ): SendSøknad {
        return SendSøknad(norskIdent, søknadId)
    }

    private suspend fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId()
    ): OasSoknadsfeil {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val response = client.postAndAssert(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(innsendingForOpprettelseAvMappe),
            api,
            søknadTypeUri
        )

        val location = response.headers().asHttpHeaders().location
        Assertions.assertEquals(HttpStatus.CREATED, response.statusCode())
        Assertions.assertNotNull(location)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt: OmsorgspengerKroniskSyktBarnSøknadDto = client.putAndAssert(
            norskIdent = null,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            requestBody = BodyInserters.fromValue(soeknadJson),
            api,
            søknadTypeUri,
            "oppdater"
        )

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = journalpostRepository.kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        val body = client.postAndAssertAwaitWithStatusAndBody<SendSøknad, OasSoknadsfeil>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            navNorskIdentHeader = null,
            assertStatus = HttpStatus.ACCEPTED,
            requestBody = BodyInserters.fromValue(sendSøknad),
            api,
            søknadTypeUri,
            "send"
        )

        return body
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId()
    ): OmsorgspengerKroniskSyktBarnSøknadDto {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val resPost = client.postAndAssert(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.CREATED,
            requestBody = BodyInserters.fromValue(innsendingForOpprettelseAvMappe),
            api,
            søknadTypeUri
        )

        val location = resPost.headers().asHttpHeaders().location
        Assertions.assertNotNull(location)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = client.putAndAssert<SøknadJson, OmsorgspengerKroniskSyktBarnSøknadDto>(
            norskIdent = null,
            authorizationHeader = saksbehandlerAuthorizationHeader,
            assertStatus = HttpStatus.OK,
            requestBody = BodyInserters.fromValue(soeknadJson),
            api,
            søknadTypeUri,
            "oppdater"
        )

        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)
        return søknadDtoFyltUt
    }
}
*/