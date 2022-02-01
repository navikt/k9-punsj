package no.nav.k9punsj

import com.fasterxml.jackson.module.kotlin.convertValue
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.Omsorg
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.domenetjenester.mappers.MapPsbTilK9Format
import no.nav.k9punsj.rest.web.OpprettNySøknad
import no.nav.k9punsj.rest.web.SendSøknad
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.*
import no.nav.k9punsj.rest.web.openapi.OasFeil
import no.nav.k9punsj.rest.web.openapi.OasSoknadsfeil
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import java.net.URI
import java.time.Duration
import java.time.LocalDate

@ExtendWith(SpringExtension::class, MockKExtension::class)
class PleiepengersyktbarnTests {

    private val client = TestSetup.client
    private val api = "api"
    private val søknadTypeUri = FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `Får tom liste når personen ikke har en eksisterende mappe`() : Unit = runBlocking {
        val norskIdent = "01110050053"
        val (status, body) = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitStatusWithBody<SvarPsbDto>()
        assertEquals(HttpStatus.OK, status)
        assertTrue(body.søknader!!.isEmpty())
    }

    @Test
    fun `Opprette ny mappe på person`() : Unit = runBlocking {
        val norskIdent = "01010050053"
        val opprettNySøknad = opprettSøknad(norskIdent, "999")
        val status = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitStatuscode()
        assertEquals(HttpStatus.CREATED, status)
    }

    @Test
    fun `Hente eksisterende mappe på person`() : Unit = runBlocking {
        val norskIdent = "02020050163"
        val opprettNySøknad = opprettSøknad(norskIdent, "9999")

        val status = client.post()
            .uri { it.pathSegment(api, søknadTypeUri).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(opprettNySøknad))
            .awaitStatuscode()
        assertEquals(HttpStatus.CREATED, status)

        val (httpStatus, body) = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitStatusWithBody<SvarPsbDto>()
        assertEquals(HttpStatus.OK, httpStatus)

        val journalposterDto = body.søknader?.first()?.journalposter
        assertEquals("9999", journalposterDto?.first())
    }

    @Test
    fun `Hent en søknad`() : Unit = runBlocking {
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

        val søknadViaGet = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", hentSøknadId(location)).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitBodyWithType<PleiepengerSyktBarnSøknadDto>()

        assertNotNull(søknadViaGet)
        assertEquals(journalpostid, søknadViaGet.journalposter?.first())
    }

    @Test
    fun `Oppdaterer en søknad`() : Unit = runBlocking {
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

        val (httpstatus, oppdatertSoeknadDto) = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(søknadFraFrontend))
            .awaitStatusWithBody<PleiepengerSyktBarnSøknadDto>()

        assertNotNull(oppdatertSoeknadDto)
        assertEquals(norskIdent, oppdatertSoeknadDto.soekerId)
        assertEquals(listOf(PeriodeDto(
            LocalDate.of(2018, 12, 30),
            LocalDate.of(2019, 10, 20))),
            oppdatertSoeknadDto.soeknadsperiode)
        assertEquals(HttpStatus.OK, httpstatus)
    }


    @Test
    fun `Innsending av søknad returnerer 404 når mappe ikke finnes`() : Unit = runBlocking {
        val norskIdent = "12030050163"
        val søknadId = "d8e2c5a8-b993-4d2d-9cb5-fdb22a653a0c"

        val sendSøknad = lagSendSøknad(norskIdent = norskIdent, søknadId = søknadId)

        val status = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "send").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(sendSøknad))
            .awaitStatuscode()

        assertEquals(HttpStatus.BAD_REQUEST, status)
    }

    @Test
    fun `sjekker at mapping fungre hele veien`() : Unit = runBlocking {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()

        val visningDto = objectMapper().convertValue<PleiepengerSyktBarnSøknadDto>(gyldigSoeknad)
        val mapTilSendingsformat = MapPsbTilK9Format(
            søknadId = visningDto.soeknadId,
            journalpostIder = visningDto.journalposter?.toSet() ?: emptySet(),
            perioderSomFinnesIK9 = emptyList(),
            dto = visningDto
        ).søknadOgFeil()
        assertNotNull(mapTilSendingsformat)

        val tilbake = objectMapper().convertValue<SøknadJson>(visningDto)
        assertEquals(visningDto.soekerId, tilbake["soekerId"].toString())
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`() : Unit = runBlocking {
        val norskIdent = "02020050121"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent)

        val (_, status, body) = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid = "9999")
        assertThat(body.feil).isNull()
        assertEquals(HttpStatus.ACCEPTED, status)

        assertThat(DatabaseUtil.getJournalpostRepo().kanSendeInn(listOf("9999"))).isFalse
    }

    @Test
    fun `Skal få 409 når det blir sendt på en journalpost som er sendt fra før, og innsendingen ikke inneholder andre journalposter som kan sendes inn`() : Unit = runBlocking {
        val norskIdent = "02020050121"
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        val journalpostId = "34234234"
        tilpasserSøknadsMalTilTesten(gyldigSoeknad, norskIdent, journalpostId)
        val (id, status, body) =
            opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent, journalpostid = journalpostId)

        assertThat(body.feil).isNull()
        assertEquals(HttpStatus.ACCEPTED, status)
        assertThat(DatabaseUtil.getJournalpostRepo().kanSendeInn(listOf(journalpostId))).isFalse

        val sendSøknad = lagSendSøknad(norskIdent = norskIdent, søknadId = id)
        val (httpstatus, body2) = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "send").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(sendSøknad))
            .awaitStatusWithBody<OasFeil>()

        assertEquals(HttpStatus.CONFLICT, httpstatus)
        assertThat(body2.feil).isEqualTo("Innsendingen må inneholde minst en journalpost som kan sendes inn.")
    }

    @Test
    fun `Skal kunne lagre ned minimal søknad`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.minimalSøknad()
        val journalpostId = IdGenerator.nesteId()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent, journalpostId)

        val (_, status, body) = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent, journalpostId)

        assertEquals(HttpStatus.BAD_REQUEST, status)
        assertThat(body.feil).isNotEmpty
    }

    @Test
    fun `Skal kunne lagre ned tomt land søknad`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.tomtLand()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val (_, status, body) = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)

        assertThat(body.feil).isNull()
        assertEquals(HttpStatus.ACCEPTED, status)
    }

    @Test
    fun `Skal kunne lagre med tid søknad`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.tidSøknad()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val (_, status, body) = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)

        assertEquals(HttpStatus.BAD_REQUEST, status)
        assertThat(body.feil).isNotEmpty
    }

    @Test
    fun `Skal kunne lagre og sette uttak`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.utenUttak()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val (_, status, body) = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)

        assertThat(body.feil).isNull()
        assertEquals(HttpStatus.ACCEPTED, status)
    }


    @Test
    fun `Skal kunne lagre med ferie null`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.ferieNull()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val (_, status, body) = opprettOgSendInnSoeknad(soeknadJson = soeknad, ident = norskIdent)

        assertThat(body.feil).isNull()
        assertEquals(HttpStatus.ACCEPTED, status)
    }

    @Test
    fun `Skal kunne lagre ned ferie fra søknad`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.ferieSøknad()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val søknadViaGet = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", oppdatertSoeknadDto.soeknadId).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitBodyWithType<PleiepengerSyktBarnSøknadDto>()

        assertNotNull(søknadViaGet)
        assertEquals(søknadViaGet.lovbestemtFerie?.get(0)?.fom!!, LocalDate.of(2021, 4, 14))
    }

    @Test
    fun `Skal kunne lagre ned sn fra søknad`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.sn()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val søknadViaGet = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", oppdatertSoeknadDto.soeknadId).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitBodyWithType<PleiepengerSyktBarnSøknadDto>()

        assertNotNull(søknadViaGet)
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.virksomhetNavn).isEqualTo("FiskerAS")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.organisasjonsnummer).isEqualTo("890508087")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.periode?.fom).isEqualTo(
            LocalDate.of(2021, 5, 10))
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.landkode).isEqualTo("")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.regnskapsførerNavn).isEqualTo("Regskapsfører")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.regnskapsførerTlf).isEqualTo("88888889")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.registrertIUtlandet).isEqualTo(
            false)
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.bruttoInntekt).isEqualTo("1200000")
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.erNyoppstartet).isEqualTo(false)
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.virksomhetstyper).isEqualTo(
            listOf("Fiske", "Jordbruk", "Dagmamma i eget hjem/familiebarnehage", "Annen næringsvirksomhet"))
    }

    @Test
    fun `Skal kunne lagre flagg om medisinske og punsjet`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val søknadViaGet = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", oppdatertSoeknadDto.soeknadId).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitBodyWithType<PleiepengerSyktBarnSøknadDto>()

        assertNotNull(søknadViaGet)
        assertThat(søknadViaGet.harInfoSomIkkeKanPunsjes).isEqualTo(true)
        assertThat(søknadViaGet.harMedisinskeOpplysninger).isEqualTo(false)
    }

    @Test
    fun `Skal verifisere at søknad er ok`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)
        opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val httpStatus = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "valider").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknad))
            .awaitStatuscode()

        assertEquals(HttpStatus.ACCEPTED, httpStatus)
    }

    @Test
    fun `Skal verifisere at vi utvider men flere journalposter`() : Unit = runBlocking {
        val norskIdent = "02022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val søknadId = opprettSoeknad(ident = norskIdent)
        leggerPåNySøknadId(soeknad, søknadId)

        client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknad))
            .awaitExchangeBlocking()

        val med2: SøknadJson = LesFraFilUtil.søknadFraFrontendMed2()
        tilpasserSøknadsMalTilTesten(med2, norskIdent)
        leggerPåNySøknadId(med2, søknadId)

        client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(med2))
            .awaitExchangeBlocking()

        val id = hentSøknadId(søknadId)

        val søknadViaGet = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", id).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitBodyWithType<PleiepengerSyktBarnSøknadDto>()

        assertThat(søknadViaGet.journalposter).hasSize(2)
        assertThat(søknadViaGet.journalposter).isEqualTo(listOf("9999", "10000"))
    }

    @Test
    fun `Skal verifisere at alle felter blir lagret`() : Unit = runBlocking {
        val norskIdent = "12022352121"
        val soeknad: SøknadJson = LesFraFilUtil.søknadFraFrontend()
        tilpasserSøknadsMalTilTesten(soeknad, norskIdent)

        val oppdatertSoeknadDto = opprettOgLagreSoeknad(soeknadJson = soeknad, ident = norskIdent)

        val søknadViaGet = client.get()
            .uri { it.pathSegment(api, søknadTypeUri, "mappe", oppdatertSoeknadDto.soeknadId).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .awaitBodyWithType<PleiepengerSyktBarnSøknadDto>()

        // GUI format
        assertNotNull(søknadViaGet)
        assertThat(søknadViaGet.soekerId).isEqualTo(norskIdent)
        assertThat(søknadViaGet.journalposter!![0]).isEqualTo("9999")
        assertThat(søknadViaGet.mottattDato).isEqualTo(LocalDate.of(2020, 10, 12))
        assertThat(søknadViaGet.barn?.norskIdent).isEqualTo("22222222222")
        assertThat(søknadViaGet.soeknadsperiode?.first()?.fom).isEqualTo(LocalDate.of(2018, 12, 30))
        assertThat(søknadViaGet.soeknadsperiode?.first()?.tom).isEqualTo(LocalDate.of(2019, 10, 20))
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.periode?.fom).isEqualTo(
            LocalDate.of(2018, 12, 30))
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.periode?.tom).isNull()
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.info?.virksomhetstyper).hasSize(4)
        assertThat(søknadViaGet.opptjeningAktivitet?.selvstendigNaeringsdrivende?.virksomhetNavn).isEqualTo("FiskerAS")
        assertThat(søknadViaGet.opptjeningAktivitet?.frilanser?.startdato).isEqualTo("2019-10-10")
        assertThat(søknadViaGet.opptjeningAktivitet?.arbeidstaker!![0].organisasjonsnummer).isEqualTo("910909088")
        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].organisasjonsnummer).isEqualTo("910909088")
        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].periode?.fom).isEqualTo(
            LocalDate.of(2018, 12, 30))
        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].periode?.tom).isEqualTo(
            LocalDate.of(2019, 10, 20))
        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].faktiskArbeidTimerPerDag).isEqualTo(
            "7,48")
        assertThat(søknadViaGet.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo?.perioder!![0].jobberNormaltTimerPerDag).isEqualTo(
            "7,48")
        assertThat(søknadViaGet.arbeidstid?.frilanserArbeidstidInfo!!.perioder?.first()?.periode?.fom).isEqualTo(
            LocalDate.of(2018,
                12,
                30))
        assertThat(søknadViaGet.arbeidstid?.selvstendigNæringsdrivendeArbeidstidInfo!!.perioder?.first()?.jobberNormaltTimerPerDag).isEqualTo(
            "7")
        assertThat(søknadViaGet.beredskap?.first()?.tilleggsinformasjon).isEqualTo("FÅ SLUTT PÅ COVID!!!")
        assertThat(søknadViaGet.nattevaak?.first()?.tilleggsinformasjon).isEqualTo("FÅ SLUTT PÅ COVID!!!")
        assertThat(søknadViaGet.tilsynsordning?.perioder?.first()?.timer).isEqualTo(7)
        assertThat(søknadViaGet.tilsynsordning?.perioder?.first()?.minutter).isEqualTo(30)
        assertThat(søknadViaGet.uttak?.first()?.timerPleieAvBarnetPerDag).isEqualTo("7,5")
        assertThat(søknadViaGet.omsorg?.relasjonTilBarnet).isEqualTo("MOR")
        assertThat(søknadViaGet.bosteder!![0].land).isEqualTo("RU")
        assertThat(søknadViaGet.lovbestemtFerie!![0].fom).isEqualTo(LocalDate.of(2018, 12, 30))
        assertThat(søknadViaGet.utenlandsopphold!![0].periode?.fom).isEqualTo(LocalDate.of(2018, 12, 30))
        assertThat(søknadViaGet.soknadsinfo!!.harMedsoeker).isEqualTo(true)
        assertThat(søknadViaGet.soknadsinfo!!.samtidigHjemme).isEqualTo(true)

        // k9-format, faktisk søknad format
        val mapTilEksternFormat = MapPsbTilK9Format(
            søknadViaGet.soeknadId,
            søknadViaGet.journalposter!!.toSet(),
            emptyList(),
            søknadViaGet
        )

        assertThat(mapTilEksternFormat.feil()).isEmpty()
        val søknad = mapTilEksternFormat.søknad()

        assertThat(søknad.søker.personIdent.verdi).isEqualTo(norskIdent)
        val ytelse = søknad.getYtelse<PleiepengerSyktBarn>()

        assertThat(ytelse.barn.personIdent.verdi).isEqualTo("22222222222")
        assertThat(ytelse.søknadsperiode.iso8601).isEqualTo("2018-12-30/2019-10-20")
        assertThat(ytelse.opptjeningAktivitet.selvstendigNæringsdrivende?.get(0)?.perioder?.keys?.first()?.iso8601).isEqualTo(
            "2018-12-30/..")
        assertThat(ytelse?.opptjeningAktivitet?.selvstendigNæringsdrivende?.get(0)?.perioder?.values?.first()?.virksomhetstyper).hasSize(
            4)
        assertThat(ytelse?.opptjeningAktivitet?.selvstendigNæringsdrivende?.get(0)?.virksomhetNavn).isEqualTo("FiskerAS")
        assertThat(ytelse.opptjeningAktivitet?.frilanser?.startdato).isEqualTo("2019-10-10")
        assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].organisasjonsnummer.verdi).isEqualTo("910909088")
        assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo.perioder?.keys?.first()?.iso8601).isEqualTo("2018-12-30/2019-10-20")
        assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo.perioder?.values?.first()?.faktiskArbeidTimerPerDag?.toString()).isEqualTo(
            "PT7H29M")
        assertThat(ytelse.arbeidstid?.arbeidstakerList!![0].arbeidstidInfo.perioder?.values?.first()?.jobberNormaltTimerPerDag?.toString()).isEqualTo(
            "PT7H29M")
        assertThat(ytelse.arbeidstid?.selvstendigNæringsdrivendeArbeidstidInfo!!.get().perioder?.values?.first()?.jobberNormaltTimerPerDag).isEqualTo(
            Duration.ofHours(7))
        assertThat(ytelse.arbeidstid?.frilanserArbeidstidInfo!!.get().perioder?.keys?.first()?.iso8601).isEqualTo("2018-12-30/2019-10-20")
        assertThat(ytelse.beredskap?.perioder?.values?.first()?.tilleggsinformasjon).isEqualTo("FÅ SLUTT PÅ COVID!!!")
        assertThat(ytelse.nattevåk?.perioder?.values?.first()?.tilleggsinformasjon).isEqualTo("FÅ SLUTT PÅ COVID!!!")
        assertThat(ytelse.tilsynsordning?.perioder?.values?.first()?.etablertTilsynTimerPerDag.toString()).isEqualTo("PT7H30M")
        assertThat(ytelse.uttak?.perioder?.values?.first()?.timerPleieAvBarnetPerDag.toString()).isEqualTo("PT7H30M")
        assertThat(ytelse.omsorg.relasjonTilBarnet.get()).isEqualTo(Omsorg.BarnRelasjon.MOR)
        assertThat(ytelse.bosteder.perioder.values.first().land.landkode).isEqualTo("RU")
        assertThat(ytelse.lovbestemtFerie!!.perioder?.get(Periode("2018-12-30/2019-06-20"))?.isSkalHaFerie).isEqualTo(
            true)
        assertThat(ytelse.lovbestemtFerie!!.perioder?.get(Periode("2019-06-21/2019-10-20"))?.isSkalHaFerie).isEqualTo(
            false)
        assertThat(ytelse.utenlandsopphold!!.perioder.keys.first()?.iso8601).isEqualTo("2018-12-30/2019-01-08")
        assertThat(ytelse.utenlandsopphold!!.perioder.values.first()?.Årsak).isEqualTo(Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD)
        assertThat(ytelse.søknadInfo!!.get().samtidigHjemme).isEqualTo(true)
        assertThat(ytelse.søknadInfo!!.get().harMedsøker).isEqualTo(true)
        assertThat(ytelse.opptjeningAktivitet.frilanser.startdato).isEqualTo(LocalDate.of(2019, 10, 10))
        assertThat(ytelse.opptjeningAktivitet.frilanser.sluttdato).isEqualTo(LocalDate.of(2019, 11, 10))
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
        assertEquals(HttpStatus.CREATED, resPost.statusCode())
        assertNotNull(location)

        leggerPåNySøknadId(soeknadJson, location)

        // fyller ut en søknad
        val søknadDtoFyltUt = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknadJson))
            .awaitBodyWithType<PleiepengerSyktBarnSøknadDto>()

        assertNotNull(søknadDtoFyltUt.soekerId)

        val søknadId = søknadDtoFyltUt.soeknadId
        val sendSøknad = lagSendSøknad(norskIdent = ident, søknadId = søknadId)

        val journalposter = søknadDtoFyltUt.journalposter!!

        val kanSendeInn = DatabaseUtil.getJournalpostRepo().kanSendeInn(journalposter)
        assertThat(kanSendeInn).isTrue

        // sender en søknad
        val (httpstatus, oasSoknadsfeil) = client.post()
            .uri { it.pathSegment(api, søknadTypeUri, "send").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(sendSøknad))
            .awaitStatusWithBody<OasSoknadsfeil>()
        return Triple(søknadId, httpstatus, oasSoknadsfeil)
    }

    private suspend fun opprettOgLagreSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): PleiepengerSyktBarnSøknadDto {
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
        val søknadDtoFyltUt = client.put()
            .uri { it.pathSegment(api, søknadTypeUri, "oppdater").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .body(BodyInserters.fromValue(soeknadJson))
            .awaitBodyWithType<PleiepengerSyktBarnSøknadDto>()

        assertNotNull(søknadDtoFyltUt.soekerId)
        return søknadDtoFyltUt
    }

    private suspend fun opprettSoeknad(
        ident: String,
        journalpostid: String = IdGenerator.nesteId(),
    ): URI? {
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

        return location
    }
}


private fun opprettSøknad(
    personnummer: NorskIdentDto,
    journalpostId: String,
): OpprettNySøknad {
    return OpprettNySøknad(personnummer, journalpostId, null, null)
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
