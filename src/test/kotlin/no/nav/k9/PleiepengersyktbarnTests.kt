package no.nav.k9

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9.mappe.MapperSvarDTO
import no.nav.k9.openapi.OasPleiepengerSyktBarSoknadMappeSvar
import no.nav.k9.pleiepengersyktbarn.soknad.*
import org.apache.kafka.common.KafkaException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import java.time.Duration
import java.time.LocalDate

@ExtendWith(SpringExtension::class, MockKExtension::class)
class PleiepengersyktbarnTests {

    private val client = TestSetup.client
    private val api = "api"
    private val søknad = "pleiepenger-sykt-barn-soknad"

    // Standardverdier for test
    private val standardIdent = "01122334410"
    private val standardSpraak: Språk = Språk.nb
    private val standardDatoMottatt: LocalDate = LocalDate.of(2020, 2, 29)
    private val standardFraOgMed: LocalDate = LocalDate.of(2020, 3, 1)
    private val standardTilOgMed: LocalDate = LocalDate.of(2020, 3, 31)
    private val standardPeriode: Periode = Periode(standardFraOgMed, standardTilOgMed)
    private val standardPerioder: List<Periode> = listOf(standardPeriode)
    private val standardBarnetsIdent: String = "29022050115"
    private val standardBarn: Barn = Barn(norskIdent = standardBarnetsIdent, foedselsdato = null)
    private val standardGrad: Float = 100.00F
    private val standardTilstedevaerelsesgrad: Tilstedevaerelsesgrad = Tilstedevaerelsesgrad(standardPeriode, standardGrad)
    private val standardOrganisasjonsnummer: String = "123456785"
    private val standardArbeidsgiver: Arbeidsgiver = Arbeidsgiver(skalJobbeProsent = listOf(standardTilstedevaerelsesgrad), organisasjonsnummer = standardOrganisasjonsnummer, norskIdent = null)
    private val standardSelvstendigNaeringsdrivende: Oppdragsforhold = Oppdragsforhold(standardPeriode)
    private val standardFrilanser: Oppdragsforhold = Oppdragsforhold(standardPeriode)
    private val standardArbeid: Arbeid = Arbeid(arbeidstaker = listOf(standardArbeidsgiver), selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende), frilanser = listOf(standardFrilanser))
    private val standardTilleggsinformasjon: String = "Lorem ipsum dolor sit amet."
    private val standardBeredskap: List<PeriodeMedTilleggsinformasjon> = listOf(PeriodeMedTilleggsinformasjon(standardTilleggsinformasjon, standardPeriode))
    private val standardNattevaak: List<PeriodeMedTilleggsinformasjon> = listOf(PeriodeMedTilleggsinformasjon(standardTilleggsinformasjon, standardPeriode))
    private val standardITilsynsordning: JaNeiVetikke = JaNeiVetikke.ja
    private val standardTilsynsvarighet: Duration = Duration.ofHours(8)
    private val standardTilsynsordningspphold: Opphold = Opphold(periode = standardPeriode, mandag = standardTilsynsvarighet, tirsdag = standardTilsynsvarighet, onsdag = standardTilsynsvarighet, torsdag = standardTilsynsvarighet, fredag = standardTilsynsvarighet)
    private val standardTilsynsordning: Tilsynsordning = Tilsynsordning(iTilsynsordning = standardITilsynsordning, opphold = listOf(standardTilsynsordningspphold))

    private fun genererSoknad(
            datoMottatt: LocalDate? = standardDatoMottatt,
            perioder: List<Periode>? = standardPerioder,
            spraak: Språk? = standardSpraak,
            barn: Barn? = standardBarn,
            beredskap: List<PeriodeMedTilleggsinformasjon>? = standardBeredskap,
            nattevaak: List<PeriodeMedTilleggsinformasjon>? = standardNattevaak,
            tilsynsordning: Tilsynsordning? = standardTilsynsordning,
            arbeid: Arbeid? = standardArbeid
    ): SøknadJson {
        return mutableMapOf(
                "datoMottatt" to genererDato(datoMottatt),
                "perioder" to perioder?.map{genererPeriode(it)},
                "spraak" to spraak,
                "barn" to if (barn == null) null else mutableMapOf("norskIdent" to barn.norskIdent, "foedselsdato" to barn.foedselsdato),
                "beredskap" to beredskap,
                "nattevaak" to nattevaak,
                "tilsynsordning" to if (tilsynsordning == null) null else mutableMapOf("iTilsynsordning" to tilsynsordning.iTilsynsordning, "opphold" to tilsynsordning.opphold),
                "arbeid" to arbeid
        )
    }

    private fun genererDato (dato: LocalDate?): ArrayList<Int>? {
        return if (dato == null) null else arrayListOf(dato.year, dato.monthValue, dato.dayOfMonth)
    }

    private fun genererPeriode (periode: Periode?): MutableMap<String, ArrayList<Int>?>? {
        return if (periode == null) null else mutableMapOf("fraOgMed" to genererDato(periode.fraOgMed), "tilOgMed" to genererDato(periode.tilOgMed))
    }

    @Test
    fun `Hente eksisterende mapper`() {
        val res = client.get()
                .uri{ it.pathSegment(api, søknad, "mapper").build() }
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())
    }

    @Test
    fun `Opprette ny mappe uten person`() {
        val innsending = Innsending(personer = mutableMapOf())
        val res = client.post()
                .uri{ it.pathSegment(api, søknad).build() }
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, res.statusCode())
    }

    @Test
    fun `Opprette ny mappe på person`() {
        val innsending = lagInnsending("01010050053", "999")
        val res = client.post()
                .uri{ it.pathSegment(api, søknad).build() }
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, res.statusCode())
    }

    @Test
    fun `Hente eksisterende mappe på person`() {

        val norskIdent = "02020050163"
        val innsending = lagInnsending(norskIdent, "9999")

        val resPost = client.post()
                .uri{ it.pathSegment(api, søknad).build() }
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, resPost.statusCode())

        val res = client.get()
                .uri{ it.pathSegment(api, søknad, "mapper").build() }
                .header("X-Nav-NorskIdent", norskIdent)
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())

        val mapperSvar = runBlocking { res.awaitBody<MapperSvarDTO>() }
        val personerSvar = mapperSvar.mapper.first().personer[norskIdent]
        assertEquals("9999", personerSvar?.innsendinger?.first())
    }

    @Test
    fun `Oppdaterer en søknad`() {

        val journalpostid = "21707da8-a13b-4927-8776-c53399727b29"
        val norskIdent = "22110055998"
        val innsendingForOpprettelseAvMappe = lagInnsending(norskIdent, journalpostid)

        val opprettetMappe = client.post()
                .uri{it.pathSegment(api, søknad).build()}
                .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
                .awaitExchangeBlocking()
                .bodyToMono(OasPleiepengerSyktBarSoknadMappeSvar::class.java)
                .block()

        assertNotNull(opprettetMappe)
        val mappeid: String = opprettetMappe!!.mappeId

        val innsendingForOppdateringAvSoeknad = lagInnsending(norskIdent, journalpostid, genererSoknad())

        val res = client.put()
                .uri{it.pathSegment(api, søknad, "mappe", mappeid).build()}
                .body(BodyInserters.fromValue(innsendingForOppdateringAvSoeknad))
                .awaitExchangeBlocking()

        val oppdatertSoeknad = res
                .bodyToMono(OasPleiepengerSyktBarSoknadMappeSvar::class.java)
                .block()
                ?.personer
                ?.get(norskIdent)
                ?.soeknad

        assertNotNull(oppdatertSoeknad)
        assertEquals(standardArbeid, oppdatertSoeknad!!.arbeid)
        assertEquals(standardBarn, oppdatertSoeknad.barn)
        assertEquals(standardBeredskap, oppdatertSoeknad.beredskap)
        assertEquals(standardDatoMottatt, oppdatertSoeknad.datoMottatt)
        assertEquals(standardNattevaak, oppdatertSoeknad.nattevaak)
        assertEquals(standardPerioder, oppdatertSoeknad.perioder)
        assertEquals(standardSpraak, oppdatertSoeknad.spraak)
        assertEquals(standardTilsynsordning, oppdatertSoeknad.tilsynsordning)

        assertEquals(HttpStatus.OK, res.statusCode())
    }

    @Test
    fun `Innsending av søknad returnerer 404 når mappe ikke finnes`() {

        val journalpostid = "2948688b-3ee6-4c05-b179-31830dde5069"
        val mappeid = "d8e2c5a8-b993-4d2d-9cb5-fdb22a653a0c"

        val innsending = lagInnsending(standardIdent, journalpostid)

        val res = client.post()
                .uri{it.pathSegment(api, søknad, "mappe", mappeid).build()}
                .header("X-Nav-NorskIdent", standardIdent)
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()

        assertEquals(HttpStatus.NOT_FOUND, res.statusCode())
    }

    @Disabled // TODO: TSF-1185: PleiepengerSyktBarnConverter mangler obligatorisk felt på output
    @Test
    fun `Prøver å sende søknaden til Kafka når den er gyldig`() {
        val gyldigSoeknad: SøknadJson = genererSoknad()
        val res = opprettOgSendInnSoeknad(gyldigSoeknad)
        assertNotNull(res.bodyToMono(KafkaException::class.java).block())
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.statusCode())
    }

    @Test
    fun `Innsending av søknad uten perioder blir stoppet i første valideringsfase`() {
        val soeknadUtenPerioder: SøknadJson = genererSoknad(perioder = emptyList())
        val res = opprettOgSendInnSoeknad(soeknadUtenPerioder)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med periode uten startdato blir stoppet i første valideringsfase`() {
        val soeknadMedPeriodeUtenStartdato: SøknadJson = genererSoknad(perioder = listOf(Periode(fraOgMed = null, tilOgMed = standardTilOgMed)))
        val res = opprettOgSendInnSoeknad(soeknadMedPeriodeUtenStartdato)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med periode uten sluttdato blir stoppet i første valideringsfase`() {
        val soeknadMedPeriodeUtenSluttdato: SøknadJson = genererSoknad(perioder = listOf(Periode(fraOgMed = standardFraOgMed, tilOgMed = null)))
        val res = opprettOgSendInnSoeknad(soeknadMedPeriodeUtenSluttdato)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med periode med sluttdato før startdato blir stoppet i første valideringsfase`() {
        val soeknadMedPeriodeMedSluttdatoFoerStartdato: SøknadJson = genererSoknad(perioder = listOf(Periode(fraOgMed = standardTilOgMed, tilOgMed = standardFraOgMed)))
        val res = opprettOgSendInnSoeknad(soeknadMedPeriodeMedSluttdatoFoerStartdato)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad uten språk blir stoppet i første valideringsfase`() {
        val soeknadUtenSpraak: SøknadJson = genererSoknad(spraak = null)
        val res = opprettOgSendInnSoeknad(soeknadUtenSpraak)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad uten mottakelsesdato blir stoppet i første valideringsfase`() {
        val soeknadUtenMottakelsesdato: SøknadJson = genererSoknad(datoMottatt = null)
        val res = opprettOgSendInnSoeknad(soeknadUtenMottakelsesdato)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad uten barn blir stoppet i første valideringsfase`() {
        val soeknadUtenBarn: SøknadJson = genererSoknad(barn = null)
        val res = opprettOgSendInnSoeknad(soeknadUtenBarn)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med barn uten fødselsnummer og fødselsdato blir stoppet i første valideringsfase`() {
        val soeknadMedBarnUtenFoedselsnummerOgFoedselsdato: SøknadJson = genererSoknad(barn = Barn(null, null))
        val res = opprettOgSendInnSoeknad(soeknadMedBarnUtenFoedselsnummerOgFoedselsdato)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med overlappende perioder blir stoppet i første valideringsfase`() {
        val soeknadMedOverlappendePerioder: SøknadJson = genererSoknad(perioder = listOf(standardPeriode, standardPeriode))
        val res = opprettOgSendInnSoeknad(soeknadMedOverlappendePerioder)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med uidentifisert arbeidsgiver blir stoppet i første valideringsfase`() {
        val soeknadMedUidentifisertArbeidsgiver: SøknadJson = genererSoknad(arbeid = Arbeid(
                arbeidstaker = listOf(Arbeidsgiver(listOf(standardTilstedevaerelsesgrad), null, null)),
                frilanser = listOf(standardFrilanser),
                selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende)
        ))
        val res = opprettOgSendInnSoeknad(soeknadMedUidentifisertArbeidsgiver)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med både person og organisasjon som arbeidsgiver blir stoppet i første valideringsfase`() {
        val soeknadMedBaadePersonOgOrganisasjonSomArbeidsgiver: SøknadJson = genererSoknad(arbeid = Arbeid(
                arbeidstaker = listOf(Arbeidsgiver(listOf(standardTilstedevaerelsesgrad), standardOrganisasjonsnummer, "22110010102")),
                frilanser = listOf(standardFrilanser),
                selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende)
        ))
        val res = opprettOgSendInnSoeknad(soeknadMedBaadePersonOgOrganisasjonSomArbeidsgiver)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med for høy tilstedeværelsesgrad blir stoppet i første valideringsfase`() {
        val soeknadMedForHoeyTilstedevaerelsesgrad: SøknadJson = genererSoknad(arbeid = Arbeid(
                arbeidstaker = listOf(Arbeidsgiver(listOf(Tilstedevaerelsesgrad(standardPeriode, 100.1F)), standardOrganisasjonsnummer, null)),
                frilanser = listOf(standardFrilanser),
                selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende)
        ))
        val res = opprettOgSendInnSoeknad(soeknadMedForHoeyTilstedevaerelsesgrad)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    @Test
    fun `Innsending av søknad med for lav tilstedeværelsesgrad blir stoppet i første valideringsfase`() {
        val soeknadMedForLavTilstedevaerelsesgrad: SøknadJson = genererSoknad(arbeid = Arbeid(
                arbeidstaker = listOf(Arbeidsgiver(listOf(Tilstedevaerelsesgrad(standardPeriode, -.1F)), standardOrganisasjonsnummer, null)),
                frilanser = listOf(standardFrilanser),
                selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende)
        ))
        val res = opprettOgSendInnSoeknad(soeknadMedForLavTilstedevaerelsesgrad)
        assertEquals(HttpStatus.BAD_REQUEST, res.statusCode())
    }

    private fun opprettOgSendInnSoeknad(
            soeknadJson: SøknadJson,
            ident: String = standardIdent,
            journalpostid: String = "73369b5b-d50e-47ab-8fc2-31ef35a71993"
    ): ClientResponse {

        val innsendingForOpprettelseAvMappe = lagInnsending(ident, journalpostid, soeknadJson)

        val opprettetMappe: OasPleiepengerSyktBarSoknadMappeSvar? = client.post()
                .uri{it.pathSegment(api, søknad).build()}
                .body(BodyInserters.fromValue(innsendingForOpprettelseAvMappe))
                .awaitExchangeBlocking()
                .bodyToMono(OasPleiepengerSyktBarSoknadMappeSvar::class.java)
                .block()

        assertNotNull(opprettetMappe)
        val mappeid: String = opprettetMappe!!.mappeId

        val innsendingForInnsendingAvSoknad = lagInnsending(ident, journalpostid)

        return client.post()
                .uri{it.pathSegment(api, søknad, "mappe", mappeid).build()}
                .header("X-Nav-NorskIdent", standardIdent)
                .body(BodyInserters.fromValue(innsendingForInnsendingAvSoknad))
                .awaitExchangeBlocking()
    }
}

private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }

private fun lagInnsending(personnummer: NorskIdent, journalpostId: String, søknad: SøknadJson = mutableMapOf()): Innsending {
    val person = JournalpostInnhold(journalpostId = journalpostId, soeknad = søknad)
    val personer = mutableMapOf<String, JournalpostInnhold<SøknadJson>>()
    personer[personnummer] = person

    return Innsending(personer)
}