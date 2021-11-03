package no.nav.k9punsj.rest.web.ruter

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.rest.web.OpprettNyOmsSøknad
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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@ExtendWith(SpringExtension::class, MockKExtension::class)
class OmsorgspengerRoutesTest{

    private val client = TestSetup.client
    private val api = "api"
    private val søknadTypeUri = FagsakYtelseTypeUri.OMSORGSPENGER
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"


    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`() {
        val norskIdent = "01110050053"
        val res = client.get()
            // get omsorgspenger-soknad/mappe
            .uri { it.pathSegment(api, søknadTypeUri, "mappe").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitExchangeBlocking()
        Assertions.assertEquals(HttpStatus.OK, res.statusCode())
        val svar = runBlocking { res.awaitBody<SvarPsbDto>() }
        Assertions.assertTrue(svar.søknader!!.isEmpty())
    }

    @Test
    fun `Opprette ny mappe på person`() {
        val norskIdent = "01010050053"
        val opprettNySøknad = opprettSøknad(norskIdent, UUID.randomUUID().toString())
        val res = client.post()
            // post omsorgspenger-soknad/mappe
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitExchangeBlocking()
        Assertions.assertEquals(HttpStatus.CREATED, res.statusCode())
    }

    @Test
    fun `Hente eksisterende mappe på person`() {
        val norskIdent = "02020050163"
        val journalpostId = UUID.randomUUID().toString()
        val opprettNySøknad = opprettSøknad(norskIdent, journalpostId)

        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitExchangeBlocking()
        Assertions.assertEquals(HttpStatus.CREATED, resPost.statusCode())

        val res = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitExchangeBlocking()
        Assertions.assertEquals(HttpStatus.OK, res.statusCode())

        val mappeSvar = runBlocking { res.awaitBody<SvarOmsDto>() }
        val journalposterDto = mappeSvar.søknader?.first()?.journalposter
        Assertions.assertEquals(journalpostId, journalposterDto?.first())
    }

    @Test
    fun `Hent en søknad`() {
        val søknad = LesFraFilUtil.søknadFraFrontend()
        val norskIdent = "02030050163"
        val journalpostid = UUID.randomUUID().toString()
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

        val resHent = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", hentSøknadId(location)).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitExchangeBlocking()

        val søknadViaGet = runBlocking { resHent.awaitBody<OmsorgspengerSøknadDto>() }
        Assertions.assertNotNull(søknadViaGet)
        Assertions.assertEquals(journalpostid, søknadViaGet.journalposter?.first())
    }

    @Test
    fun `Oppdaterer en søknad`() {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontendOms()
        val norskIdent = "02030050163"
        val journalpostid = UUID.randomUUID().toString()
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

        val res = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(søknadFraFrontend))
            .awaitExchangeBlocking()

        val oppdatertSoeknadDto = runBlocking { res.awaitBody<OmsorgspengerSøknadDto>() }

        Assertions.assertNotNull(oppdatertSoeknadDto)
        Assertions.assertEquals(norskIdent, oppdatertSoeknadDto.soekerId)
        Assertions.assertEquals(HttpStatus.OK, res.statusCode())
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`() {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOms()
        val journalpostid = UUID.randomUUID().toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        val res = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        val response = res.second
            .bodyToMono(OasSoknadsfeil::class.java)
            .block()
        assertThat(response?.feil).isNull()
        Assertions.assertEquals(HttpStatus.ACCEPTED, res.second.statusCode())
        assertThat(DatabaseUtil.getJournalpostRepo().kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal fordele trekk av dager på enkelt dager slik at det validere ok`() {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsTrekk()
        val journalpostid = UUID.randomUUID().toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        val res = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        val response = res.second
            .bodyToMono(OasSoknadsfeil::class.java)
            .block()
        assertThat(response?.feil).isNull()
        Assertions.assertEquals(HttpStatus.ACCEPTED, res.second.statusCode())
        assertThat(DatabaseUtil.getJournalpostRepo().kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal verifisere at søknad er ok`() {
        val norskIdent = "02022352122"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOms()
        val journalpostid = UUID.randomUUID().toString()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostid)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostid)

        val res = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "valider").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknad))
            .awaitExchangeBlocking()

        Assertions.assertEquals(HttpStatus.ACCEPTED, res.statusCode())
    }

    @Test
    fun `Skal fordele trekk av dager på enkelt dager slik at det validere ok - kompleks versjon`() {
        val norskIdent = "02020050123"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontendOmsTrekkKompleks()
        val journalpostid = UUID.randomUUID().toString()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostid)

        val res = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid)
        val response = res.second
            .bodyToMono(OasSoknadsfeil::class.java)
            .block()
        assertThat(response?.feil).isNull()
        Assertions.assertEquals(HttpStatus.ACCEPTED, res.second.statusCode())
        assertThat(DatabaseUtil.getJournalpostRepo().kanSendeInn(listOf(journalpostid))).isFalse
    }

    @Test
    fun `Skal hente arbeidsforholdIder fra k9-sak`() {
        val norskIdent = "02020050123"
        val dtoSpørring =
            OasMatchfagsakMedPeriode(norskIdent, PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1)))

        val res = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "k9sak", "arbeidsforholdIder").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(dtoSpørring))
            .awaitExchangeBlocking()

        val oppdatertSoeknadDto = runBlocking { res.awaitBody<List<ArbeidsgiverMedArbeidsforholdId>>() }

        Assertions.assertEquals("randomArbeidsforholdId", oppdatertSoeknadDto[0].arbeidsforholdId[0])
    }

    private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }

    private fun opprettSøknad(
        personnummer: NorskIdentDto,
        journalpostId: String,
    ): OpprettNyOmsSøknad {
        return OpprettNyOmsSøknad(personnummer, journalpostId)
    }

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

    private fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): Pair<SøknadIdDto, ClientResponse> {
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
        val resPut = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknadJson))
            .awaitExchangeBlocking()

        val søknadDtoFyltUt = runBlocking { resPut.awaitBody<OmsorgspengerSøknadDto>() }
        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = DatabaseUtil.getJournalpostRepo().kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        return Pair(søknadId, client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "send").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(sendSøknad))
            .awaitExchangeBlocking())
    }

    private fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): PleiepengerSøknadDto {
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
        val resPut = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknadJson))
            .awaitExchangeBlocking()

        val søknadDtoFyltUt = runBlocking { resPut.awaitBody<PleiepengerSøknadDto>() }
        Assertions.assertNotNull(søknadDtoFyltUt.soekerId)

        return søknadDtoFyltUt
    }
}
