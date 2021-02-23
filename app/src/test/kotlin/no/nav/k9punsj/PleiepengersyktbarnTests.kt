package no.nav.k9punsj

import com.fasterxml.jackson.module.kotlin.convertValue
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.rest.web.HentSøknad
import no.nav.k9punsj.rest.web.Innsending
import no.nav.k9punsj.rest.web.JournalpostInnhold
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.JournalposterDto
import no.nav.k9punsj.rest.web.dto.MapperSvarDTO
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.openapi.OasPleiepengerSyktBarSoknadMappeSvar
import no.nav.k9punsj.rest.web.openapi.OasPleiepengerSyktBarnFeil
import no.nav.k9punsj.rest.web.openapi.OasPleiepengerSyktBarnSvarV2
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
import java.time.LocalDate

@ExtendWith(SpringExtension::class, MockKExtension::class)
class PleiepengersyktbarnTests {

    private val client = TestSetup.client
    private val api = "api"
    private val søknadTypeUri = FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN

    // Standardverdier for test
    private val standardIdent = "01122334410"
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `Hente eksisterende mapper`() {
        val res = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mapper").build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())
    }

    @Test
    fun `Opprette ny mappe på person`() {
        val norskIdent = "01010050053"
        val innsending = lagInnsending(norskIdent, "999")
        val res = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(innsending))
            .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, res.statusCode())
    }

    @Test
    fun `Hente eksisterende mappe på person`() {
        val norskIdent = "02020050163"
        val innsending = lagInnsending(norskIdent, "9999")

        val resPost = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(innsending))
            .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, resPost.statusCode())

        val res = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mapper").build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())

        val mapperSvar = runBlocking { res.awaitBody<MapperSvarDTO>() }
        val journalJsonB = mapperSvar.mapper.first().bunker.first().søknader?.first()?.journalposter!!
        val journalposterDto: JournalposterDto = objectMapper().convertValue(journalJsonB)
        assertEquals("9999", journalposterDto.journalposter.first())
    }

    @Test
    fun `Oppdaterer en søknad`() {
        val søknad = LesFraFilUtil.genererKomplettSøknad()
        val norskIdent = "02030050163"
        endreSøkerIGenererSøknad(søknad, norskIdent)

        val journalpostid = "21707da8-a13b-4927-8776-c53399727b29"
        val innsendingForOpprettelseAvMappe = lagInnsending(norskIdent, journalpostid)

        val opprettetMappe = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
            .awaitExchangeBlocking()
            .bodyToMono(OasPleiepengerSyktBarSoknadMappeSvar::class.java)
            .block()

        assertNotNull(opprettetMappe)
        val mappeid: String = opprettetMappe!!.mappeId
        val søknadId: String = opprettetMappe.bunker?.first()?.søknader?.first()?.søknadId!!

        val innsendingForOppdateringAvSoeknad = lagInnsending(norskIdent, journalpostid, søknad, søknadId)

        val res = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", mappeid).build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(innsendingForOppdateringAvSoeknad))
            .awaitExchangeBlocking()

        val oppdatertSoeknad = res
            .bodyToMono(OasPleiepengerSyktBarSoknadMappeSvar::class.java)
            .block()

        val søknad1 = oppdatertSoeknad?.bunker
            ?.first()
            ?.søknader
            ?.first()
            ?.søknad


        assertNotNull(oppdatertSoeknad)
        assertEquals(norskIdent, søknad1?.søker?.norskIdentitetsnummer)

        assertEquals(HttpStatus.OK, res.statusCode())
    }

    @Test
    fun `Innsending av søknad returnerer 404 når mappe ikke finnes`() {

        val journalpostid = "2948688b-3ee6-4c05-b179-31830dde5069"
        val mappeid = "d8e2c5a8-b993-4d2d-9cb5-fdb22a653a0c"

        val innsending = lagInnsending(standardIdent, journalpostid)

        val res = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", mappeid).build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", standardIdent)
            .body(BodyInserters.fromValue(innsending))
            .awaitExchangeBlocking()

        assertEquals(HttpStatus.NOT_FOUND, res.statusCode())
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`() {
        val norskIdent = "02020050121"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.genererKomplettSøknad()
        endreSøkerIGenererSøknad(gyldigSoeknad, norskIdent)

        val res = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent)

        assertEquals(HttpStatus.ACCEPTED, res.statusCode())
    }

    @Test
    fun `Skal fange opp feilen overlappendePerioder i søknaden`() {
        val norskIdent = "02020052121"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.genererSøknadMedFeil()
        endreSøkerIGenererSøknad(gyldigSoeknad, norskIdent)

        val res = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent)

        val response = res
            .bodyToMono(OasPleiepengerSyktBarnFeil::class.java)
            .block()

        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        assertEquals("overlappendePerioder", response?.feil?.first()?.feilkode!!)
    }

    @Test
    fun `Skal hente komplett søknad fra k9-sak`() {
        val søknad = LesFraFilUtil.genererKomplettSøknad()
        val norskIdent = (søknad["søker"] as Map<*, *>)["norskIdentitetsnummer"] as String
        val hentSøknad = lagHentSøknad(norskIdent, PeriodeDto(LocalDate.of(2018, 12, 30), LocalDate.of(2019, 10,20)))

        val res = client.post()
            .uri { it.pathSegment(api, "k9-sak", søknadTypeUri).build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(hentSøknad))
            .awaitExchangeBlocking()

        val søknadDto = res
            .bodyToMono(OasPleiepengerSyktBarnSvarV2::class.java)
            .block()

        assertEquals(HttpStatus.OK, res.statusCode())
        assertEquals(søknadDto?.søker, norskIdent)
        assertEquals(søknadDto?.fagsakKode, "PSB")
        assertTrue(søknadDto?.søknader?.size == 1)
        assertTrue(søknadDto?.søknader?.get(0)?.søknadId.isNullOrBlank().not())
        assertEquals(søknadDto?.søknader?.get(0)?.søknad?.ytelse?.søknadsperiode, "2018-12-30/2019-10-20")
    }

    @Test
    fun `Innsending av søknad med feil i perioden blir stoppet`() {
        val norskIdent = "02022352121"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.genererKomplettSøknad()
        endreSøkerIGenererSøknad(gyldigSoeknad, norskIdent)

        val ytelse = gyldigSoeknad["ytelse"] as MutableMap<String, Any>

        //ødelegger perioden
        ytelse.replace("søknadsperiode", "2019-12-30/2018-10-20")
        gyldigSoeknad.replace("ytelse", ytelse)

        val res = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent)

        val response = res
            .bodyToMono(OasPleiepengerSyktBarnFeil::class.java)
            .block()
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
        assertEquals("ugyldigPeriode", response?.feil?.first()?.feilkode!!)
    }

    private fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = "73369b5b-d50e-47ab-8fc2-31ef35a71993",
    ): ClientResponse {

        val innsendingForOpprettelseAvMappe = lagInnsending(ident, journalpostid, soeknadJson)

        val opprettetMappe: OasPleiepengerSyktBarSoknadMappeSvar? = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
            .awaitExchangeBlocking()
            .bodyToMono(OasPleiepengerSyktBarSoknadMappeSvar::class.java)
            .block()

        assertNotNull(opprettetMappe)
        val mappeid: String = opprettetMappe!!.mappeId
        val søknadId = opprettetMappe!!.bunker!!.first().søknader?.first()?.søknadId!!

        val innsendingForInnsendingAvSoknad = lagInnsending(ident, journalpostid, søknadId = søknadId)

        return client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", mappeid).build() }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", ident)
            .body(BodyInserters.fromValue(innsendingForInnsendingAvSoknad))
            .awaitExchangeBlocking()
    }
}

private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }

private fun lagInnsending(
    personnummer: NorskIdentDto,
    journalpostId: String,
    søknad: SøknadJson = mutableMapOf(),
    søknadId: String? = null
): Innsending {
    val person = JournalpostInnhold(journalpostId = journalpostId, soeknad = søknad, søknadIdDto = søknadId)
    val personer = mutableMapOf<String, JournalpostInnhold<SøknadJson>>()
    personer[personnummer] = person

    return Innsending(personer)
}

private fun lagHentSøknad(norskIdentDto: NorskIdentDto, periode: PeriodeDto): HentSøknad {
    return HentSøknad(norskIdent = norskIdentDto, periode = periode)
}

private fun endreSøkerIGenererSøknad(
    søknad: MutableMap<String, Any?>,
    norskIdent: String,
) {
    val norskIdentMap = søknad["søker"] as MutableMap<String, Any>
    norskIdentMap.replace("norskIdentitetsnummer", norskIdent)
    søknad.replace("søker", norskIdentMap)
}
