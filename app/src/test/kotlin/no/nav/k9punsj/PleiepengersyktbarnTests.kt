package no.nav.k9punsj

import com.fasterxml.jackson.module.kotlin.convertValue
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.domenetjenester.mappers.SøknadMapper
import no.nav.k9punsj.rest.web.OpprettNySøknad
import no.nav.k9punsj.rest.web.SendSøknad
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.*
import no.nav.k9punsj.rest.web.openapi.OasPleiepengerSyktBarnFeil
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.Assert.*
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

@ExtendWith(SpringExtension::class, MockKExtension::class)
class PleiepengersyktbarnTests {

    private val client = TestSetup.client
    private val api = "api"
    private val søknadTypeUri = FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`() {
        val norskIdent = "01110050053"
        val res = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())
        val svar = runBlocking { res.awaitBody<SvarDto>() }
        assertTrue(svar.søknader!!.isEmpty())
    }

    @Test
    fun `Opprette ny mappe på person`() {
        val norskIdent = "01010050053"
        val opprettNySøknad = opprettSøknad(norskIdent, "999")
        val res = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, res.statusCode())
    }

    @Test
    fun `Hente eksisterende mappe på person`() {
        val norskIdent = "02020050163"
        val opprettNySøknad = opprettSøknad(norskIdent, "9999")

        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, resPost.statusCode())

        val res = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())

        val mappeSvar = runBlocking { res.awaitBody<SvarDto>() }
        val journalposterDto = mappeSvar.søknader?.first()?.journalposter
        assertEquals("9999", journalposterDto?.first())
    }

    @Test
    fun `Hent en søknad`() {
        val søknad = LesFraFilUtil.søknadFraFrontend()
        val norskIdent = "02030050163"
        val journalpostid = "21707da8-a13b-4927-8776-c53399727b29"
        tilpasserSøknadsMalTilTesten(søknad, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitExchangeBlocking()

        val location = resPost.headers().asHttpHeaders().location
        assertEquals(HttpStatus.CREATED, resPost.statusCode())
        assertNotNull(location)

        val resHent = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", hentSøknadId(location)).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitExchangeBlocking()

        val søknadViaGet = runBlocking { resHent.awaitBody<PleiepengerSøknadVisningDto>() }
        assertNotNull(søknadViaGet)
        assertEquals(journalpostid, søknadViaGet.journalposter?.first())
    }

    @Test
    fun `Oppdaterer en søknad`() {
        val søknadFraFrontend = LesFraFilUtil.søknadFraFrontend()
        val norskIdent = "02030050163"
        val journalpostid = "9999"
        tilpasserSøknadsMalTilTesten(søknadFraFrontend, norskIdent, journalpostid)

        val opprettNySøknad = opprettSøknad(norskIdent, journalpostid)

        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitExchangeBlocking()

        val location = resPost.headers().asHttpHeaders().location
        assertEquals(HttpStatus.CREATED, resPost.statusCode())
        assertNotNull(location)

        leggerPåNySøknadId(søknadFraFrontend, location)

        val res = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(søknadFraFrontend))
            .awaitExchangeBlocking()

        val oppdatertSoeknadDto = runBlocking { res.awaitBody<PleiepengerSøknadVisningDto>() }

        assertNotNull(oppdatertSoeknadDto)
        assertEquals(norskIdent, oppdatertSoeknadDto.soekerId)
        assertEquals(PeriodeDto(
            LocalDate.of(2018, 12, 30),
            LocalDate.of(2019, 10, 20)),
            oppdatertSoeknadDto.soeknadsperiode)
        assertEquals(HttpStatus.OK, res.statusCode())
    }


    @Test
    fun `Innsending av søknad returnerer 404 når mappe ikke finnes`() {
        val norskIdent = "12030050163"
        val søknadId = "d8e2c5a8-b993-4d2d-9cb5-fdb22a653a0c"

        val sendSøknad = lagSendSøknad(norskIdent = norskIdent, søknadId = søknadId)

        val res = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "send").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(sendSøknad))
            .awaitExchangeBlocking()

        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `sjekker at mapping fungre hele veien`() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()

        val visningDto = objectMapper().convertValue<PleiepengerSøknadVisningDto>(gyldigSoeknad)
        val mapTilSendingsformat = SøknadMapper.mapTilSendingsformat(visningDto)
        assertNotNull(mapTilSendingsformat)

        val tilbake = objectMapper().convertValue<SøknadJson>(visningDto)
//        assertEquals(gyldigSoeknad, tilbake)
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`() {
        val norskIdent = "02020050121"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent)

        val res = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent)
        val response = res
            .bodyToMono(OasPleiepengerSyktBarnFeil::class.java)
            .block()
        assertEquals(HttpStatus.ACCEPTED, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med feil i perioden blir stoppet`() {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val periode = soeknad["soeknadsperiode"] as MutableMap<String, Any>
        periode.replace("fom", "2019-12-30")
        periode.replace("tom", "2018-10-20")

        //ødelegger perioden
        soeknad.replace("soeknadsperiode", periode)

        val res = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val response = res
            .bodyToMono(OasPleiepengerSyktBarnFeil::class.java)
            .block()
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        //TODO fix når det er rettet i k9-format
        assertEquals("nullIllegalArgumentException", response?.feil?.first()?.feilkode!!)
    }

    @Test
    fun `Skal kunne lagre ned minimal søknad`() {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.minimalSøknad()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val res = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val response = res
            .bodyToMono(OasPleiepengerSyktBarnFeil::class.java)
            .block()
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        //7 feil
        assertEquals(response?.feil?.size, 5)
    }

    @Test
    fun `Skal kunne lagre med tid søknad`() {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.tidSøknad()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val res = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val response = res
            .bodyToMono(OasPleiepengerSyktBarnFeil::class.java)
            .block()
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        //7 feil
        assertEquals(response?.feil?.size, 7)
    }

    @Test
    fun `Skal kunne lagre og sette uttak`() {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.utenUttak()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val res = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)

        assertEquals(HttpStatus.ACCEPTED, res.statusCode())
    }


    @Test
    fun `Skal kunne lagre med ferie null`() {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.ferieNull()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val res = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)

        assertEquals(HttpStatus.ACCEPTED, res.statusCode())
    }

    @Test
    fun `Skal kunne lagre ned ferie fra søknad`() {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.ferieSøknad()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val resHent = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe",oppdatertSoeknadDto.soeknadId ).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitExchangeBlocking()

        val søknadViaGet = runBlocking { resHent.awaitBody<PleiepengerSøknadVisningDto>() }

        assertNotNull(søknadViaGet)
        assertEquals(søknadViaGet.lovbestemtFerie?.get(0)?.fom!!, LocalDate.of(2021, 4, 14))
    }

    private fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = "73369b5b-d50e-47ab-8fc2-31ef35a71993",
    ): ClientResponse {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
            .awaitExchangeBlocking()

        val location = resPost.headers().asHttpHeaders().location
        assertEquals(HttpStatus.CREATED, resPost.statusCode())
        assertNotNull(location)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val resPut = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknadJson))
            .awaitExchangeBlocking()

        val søknadDtoFyltUt = runBlocking { resPut.awaitBody<PleiepengerSøknadVisningDto>() }
        assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        // sender en søknad
        return client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "send").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(sendSøknad))
            .awaitExchangeBlocking()
    }

    private fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = "73369b5b-d50e-47ab-8fc2-31ef35a71993",
    ): PleiepengerSøknadVisningDto {
        val innsendingForOpprettelseAvMappe = opprettSøknad(ident, journalpostid)

        // oppretter en søknad
        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
            .awaitExchangeBlocking()

        val location = resPost.headers().asHttpHeaders().location
        assertEquals(HttpStatus.CREATED, resPost.statusCode())
        assertNotNull(location)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val resPut = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknadJson))
            .awaitExchangeBlocking()

        val søknadDtoFyltUt = runBlocking { resPut.awaitBody<PleiepengerSøknadVisningDto>() }
        assertNotNull(søknadDtoFyltUt.soekerId)

        return søknadDtoFyltUt
    }
}


private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }


private fun opprettSøknad(
    personnummer: NorskIdentDto,
    journalpostId: String,
): OpprettNySøknad {
    return OpprettNySøknad(personnummer, journalpostId)
}

private fun lagSendSøknad(
    norskIdent: NorskIdentDto,
    søknadId: SøknadIdDto,
): SendSøknad {
    return SendSøknad(norskIdent, søknadId)
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

private fun hentSøknadId(location: URI?): String? {
    val path = location?.path
    val søknadId = path?.substring(path.lastIndexOf('/'))
    return søknadId?.trim('/')
}

