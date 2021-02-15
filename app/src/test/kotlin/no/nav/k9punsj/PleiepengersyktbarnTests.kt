package no.nav.k9punsj

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.db.datamodell.FagsakYtelseTypeUri
import no.nav.k9punsj.rest.web.Innsending
import no.nav.k9punsj.rest.web.JournalpostInnhold
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.MapperSvarDTO
import no.nav.k9punsj.rest.web.dto.NorskIdentDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto
import no.nav.k9punsj.rest.web.openapi.OasPleiepengerSyktBarSoknadMappeSvar
import no.nav.k9punsj.rest.web.openapi.OasPleiepengerSyktBarnFeil
import no.nav.k9punsj.util.LesFraFilUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@ExtendWith(SpringExtension::class, MockKExtension::class)
class PleiepengersyktbarnTests {

    private val client = TestSetup.client
    private val api = "api"
    private val søknadTypeUri = FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN

    // Standardverdier for test
    private val standardIdent = "01122334410"


    @Test
    fun `Hente eksisterende mapper`() {
        val res = client.get()
                .uri{ it.pathSegment(api, søknadTypeUri, "mapper").build() }
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())
    }

    @Test
    fun `Opprette ny mappe uten person`() {
        val innsending = Innsending(personer = mutableMapOf())
        val res = client.post()
                .uri{ it.pathSegment(api, søknadTypeUri).build() }
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, res.statusCode())
    }

    @Test
    fun `Opprette ny mappe på person`() {
        val innsending = lagInnsending("01010050053", "999")
        val res = client.post()
                .uri{ it.pathSegment(api, søknadTypeUri).build() }
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, res.statusCode())
    }

    @Test
    fun `Hente eksisterende mappe på person`() {

        val norskIdent = "02020050163"
        val innsending = lagInnsending(norskIdent, "9999")

        val resPost = client.post()
                .uri{ it.pathSegment(api, søknadTypeUri).build() }
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, resPost.statusCode())

        val res = client.get()
                .uri{ it.pathSegment(api, søknadTypeUri, "mapper").build() }
                .header("X-Nav-NorskIdent", norskIdent)
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())

        val mapperSvar = runBlocking { res.awaitBody<MapperSvarDTO>() }
        val personerSvar = mapperSvar.mapper.first().personer[norskIdent]
        assertEquals("9999", personerSvar?.innsendinger?.first())
    }

    @Test
    fun `Oppdaterer en søknad`() {
        val søknad = LesFraFilUtil.genererKomplettSøknad()

        val journalpostid = "21707da8-a13b-4927-8776-c53399727b29"
        val norskIdent = (søknad["søker"] as Map<*, *>)["norskIdentitetsnummer"] as String
        val innsendingForOpprettelseAvMappe = lagInnsending(norskIdent, journalpostid)

        val opprettetMappe = client.post()
                .uri{it.pathSegment(api, søknadTypeUri).build()}
                .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
                .awaitExchangeBlocking()
                .bodyToMono(OasPleiepengerSyktBarSoknadMappeSvar::class.java)
                .block()

        assertNotNull(opprettetMappe)
        val mappeid: String = opprettetMappe!!.mappeId


        val innsendingForOppdateringAvSoeknad = lagInnsending(norskIdent, journalpostid, søknad)

        val res = client.put()
                .uri{it.pathSegment(api, søknadTypeUri, "mappe", mappeid).build()}
                .body(BodyInserters.fromValue(innsendingForOppdateringAvSoeknad))
                .awaitExchangeBlocking()

        val oppdatertSoeknad = res
                .bodyToMono(OasPleiepengerSyktBarSoknadMappeSvar::class.java)
                .block()
                ?.personer
                ?.get(norskIdent)
                ?.soeknad

        assertNotNull(oppdatertSoeknad)
        assertEquals(oppdatertSoeknad?.søker?.norskIdentitetsnummer, norskIdent)

        assertEquals(HttpStatus.OK, res.statusCode())
    }

    @Test
    fun `Innsending av søknad returnerer 404 når mappe ikke finnes`() {

        val journalpostid = "2948688b-3ee6-4c05-b179-31830dde5069"
        val mappeid = "d8e2c5a8-b993-4d2d-9cb5-fdb22a653a0c"

        val innsending = lagInnsending(standardIdent, journalpostid)

        val res = client.post()
                .uri{it.pathSegment(api, søknadTypeUri, "mappe", mappeid).build()}
                .header("X-Nav-NorskIdent", standardIdent)
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()

        assertEquals(HttpStatus.NOT_FOUND, res.statusCode())
    }

    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.genererKomplettSøknad()
        val norskIdent = (gyldigSoeknad["søker"] as Map<*, *>)["norskIdentitetsnummer"] as String
        val res = opprettOgSendInnSoeknad(soeknadJson = gyldigSoeknad, ident = norskIdent)

        assertEquals(HttpStatus.ACCEPTED, res.statusCode())
    }

    @Test
    fun `Skal fange opp feilen overlappendePerioder i søknaden`() {
        val gyldigSoeknad: SøknadJson = LesFraFilUtil.genererSøknadMedFeil()
        val norskIdent = (gyldigSoeknad["søker"] as Map<*, *>)["norskIdentitetsnummer"] as String
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

        val res = client.get()
            .uri{ it.pathSegment(api, "k9-sak", søknadTypeUri).build() }
            .header("X-Nav-NorskIdent", norskIdent)
            .awaitExchangeBlocking()

        val søknadDto = res
            .bodyToMono(PleiepengerSøknadDto::class.java)
            .block()

        assertEquals(HttpStatus.OK, res.statusCode())
        assertEquals(søknadDto?.søker?.norskIdentitetsnummer, norskIdent)
    }

//    @Test
//    fun `Innsending av søknad uten perioder blir stoppet i første valideringsfase`() {
//        val soeknadUtenPerioder: SøknadJson = genererKomplettSøknad(perioder = emptyList())
//        val res = opprettOgSendInnSoeknad(soeknadUtenPerioder)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad med periode uten startdato blir stoppet i første valideringsfase`() {
//        val soeknadMedPeriodeUtenStartdato: SøknadJson = genererKomplettSøknad(perioder = listOf(Periode(fraOgMed = null, tilOgMed = standardTilOgMed)))
//        val res = opprettOgSendInnSoeknad(soeknadMedPeriodeUtenStartdato)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad med periode uten sluttdato blir stoppet i første valideringsfase`() {
//        val soeknadMedPeriodeUtenSluttdato: SøknadJson = genererKomplettSøknad(perioder = listOf(Periode(fraOgMed = standardFraOgMed, tilOgMed = null)))
//        val res = opprettOgSendInnSoeknad(soeknadMedPeriodeUtenSluttdato)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad med periode med sluttdato før startdato blir stoppet i første valideringsfase`() {
//        val soeknadMedPeriodeMedSluttdatoFoerStartdato: SøknadJson = genererKomplettSøknad(perioder = listOf(Periode(fraOgMed = standardTilOgMed, tilOgMed = standardFraOgMed)))
//        val res = opprettOgSendInnSoeknad(soeknadMedPeriodeMedSluttdatoFoerStartdato)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad uten språk blir stoppet i første valideringsfase`() {
//        val soeknadUtenSpraak: SøknadJson = genererKomplettSøknad(spraak = null)
//        val res = opprettOgSendInnSoeknad(soeknadUtenSpraak)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad uten mottakelsesdato blir stoppet i første valideringsfase`() {
//        val soeknadUtenMottakelsesdato: SøknadJson = genererKomplettSøknad(datoMottatt = null)
//        val res = opprettOgSendInnSoeknad(soeknadUtenMottakelsesdato)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad uten barn blir stoppet i første valideringsfase`() {
//        val soeknadUtenBarn: SøknadJson = genererKomplettSøknad(barn = null)
//        val res = opprettOgSendInnSoeknad(soeknadUtenBarn)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad med barn uten fødselsnummer og fødselsdato blir stoppet i første valideringsfase`() {
//        val soeknadMedBarnUtenFoedselsnummerOgFoedselsdato: SøknadJson = genererKomplettSøknad(barn = Barn(null, null))
//        val res = opprettOgSendInnSoeknad(soeknadMedBarnUtenFoedselsnummerOgFoedselsdato)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad med overlappende perioder blir stoppet i første valideringsfase`() {
//        val soeknadMedOverlappendePerioder: SøknadJson = genererKomplettSøknad(perioder = listOf(standardPeriode, standardPeriode))
//        val res = opprettOgSendInnSoeknad(soeknadMedOverlappendePerioder)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad med uidentifisert arbeidsgiver blir stoppet i første valideringsfase`() {
//        val soeknadMedUidentifisertArbeidsgiver: SøknadJson = genererKomplettSøknad(arbeid = Arbeid(
//                arbeidstaker = listOf(Arbeidsgiver(listOf(standardTilstedevaerelsesgrad), null, null)),
//                frilanser = listOf(standardFrilanser),
//                selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende)
//        ))
//        val res = opprettOgSendInnSoeknad(soeknadMedUidentifisertArbeidsgiver)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad med både person og organisasjon som arbeidsgiver blir stoppet i første valideringsfase`() {
//        val soeknadMedBaadePersonOgOrganisasjonSomArbeidsgiver: SøknadJson = genererKomplettSøknad(arbeid = Arbeid(
//                arbeidstaker = listOf(Arbeidsgiver(listOf(standardTilstedevaerelsesgrad), standardOrganisasjonsnummer, "22110010102")),
//                frilanser = listOf(standardFrilanser),
//                selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende)
//        ))
//        val res = opprettOgSendInnSoeknad(soeknadMedBaadePersonOgOrganisasjonSomArbeidsgiver)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad med for høy tilstedeværelsesgrad blir stoppet i første valideringsfase`() {
//        val soeknadMedForHoeyTilstedevaerelsesgrad: SøknadJson = genererKomplettSøknad(arbeid = Arbeid(
//                arbeidstaker = listOf(Arbeidsgiver(listOf(Tilstedevaerelsesgrad(standardPeriode, 100.1F)), standardOrganisasjonsnummer, null)),
//                frilanser = listOf(standardFrilanser),
//                selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende)
//        ))
//        val res = opprettOgSendInnSoeknad(soeknadMedForHoeyTilstedevaerelsesgrad)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }
//
//    @Test
//    fun `Innsending av søknad med for lav tilstedeværelsesgrad blir stoppet i første valideringsfase`() {
//        val soeknadMedForLavTilstedevaerelsesgrad: SøknadJson = genererKomplettSøknad(arbeid = Arbeid(
//                arbeidstaker = listOf(Arbeidsgiver(listOf(Tilstedevaerelsesgrad(standardPeriode, -.1F)), standardOrganisasjonsnummer, null)),
//                frilanser = listOf(standardFrilanser),
//                selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende)
//        ))
//        val res = opprettOgSendInnSoeknad(soeknadMedForLavTilstedevaerelsesgrad)
//        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
//    }

    private fun opprettOgSendInnSoeknad(
        soeknadJson: SøknadJson,
        ident: String,
        journalpostid: String = "73369b5b-d50e-47ab-8fc2-31ef35a71993",
    ): ClientResponse {

        val innsendingForOpprettelseAvMappe = lagInnsending(ident, journalpostid, soeknadJson)

        val opprettetMappe: OasPleiepengerSyktBarSoknadMappeSvar? = client.post()
                .uri{it.pathSegment(api, søknadTypeUri).build()}
                .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
                .awaitExchangeBlocking()
                .bodyToMono(OasPleiepengerSyktBarSoknadMappeSvar::class.java)
                .block()

        assertNotNull(opprettetMappe)
        val mappeid: String = opprettetMappe!!.mappeId

        val innsendingForInnsendingAvSoknad = lagInnsending(ident, journalpostid)

        return client.post()
                .uri{it.pathSegment(api, søknadTypeUri, "mappe", mappeid).build()}
                .header("X-Nav-NorskIdent", standardIdent)
                .body(BodyInserters.fromValue(innsendingForInnsendingAvSoknad))
                .awaitExchangeBlocking()
    }
}

private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }

private fun lagInnsending(personnummer: NorskIdentDto, journalpostId: String, søknad: SøknadJson = mutableMapOf()): Innsending {
    val person = JournalpostInnhold(journalpostId = journalpostId, soeknad = søknad)
    val personer = mutableMapOf<String, JournalpostInnhold<SøknadJson>>()
    personer[personnummer] = person

    return Innsending(personer)
}
