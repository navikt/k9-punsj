package no.nav.k9punsj.pleiepengersyktbarn

import org.junit.jupiter.api.Disabled

@Disabled // TODO: TSF-1185: Denne testen feiler da converteren ikke fyller ut et obligatorisk felt
class PleiepengerSyktBarnConverterTests {

//    // Standardverdier for test
//    private val standardIdent = "01122334410"
//    private val standardSpraak: Språk = Språk.nb
//    private val standardDatoMottatt: LocalDate = LocalDate.of(2020, 2, 29)
//    private val standardFraOgMed: LocalDate = LocalDate.of(2020, 3, 1)
//    private val standardTilOgMed: LocalDate = LocalDate.of(2020, 3, 31)
//    private val standardPeriode: Periode = Periode(standardFraOgMed, standardTilOgMed)
//    private val standardPerioder: List<Periode> = listOf(standardPeriode)
//    private val standardBarnetsIdent: String = "29022050115"
//    private val standardBarn: Barn = Barn(norskIdent = standardBarnetsIdent, foedselsdato = null)
//    private val standardGrad: Float = 100.00F
//    private val standardTilstedevaerelsesgrad: Tilstedevaerelsesgrad = Tilstedevaerelsesgrad(standardPeriode, standardGrad)
//    private val standardOrganisasjonsnummer: String = "123456785"
//    private val standardArbeidsgiver: Arbeidsgiver = Arbeidsgiver(skalJobbeProsent = listOf(standardTilstedevaerelsesgrad), organisasjonsnummer = standardOrganisasjonsnummer, norskIdent = null)
//    private val standardSelvstendigNaeringsdrivende: Oppdragsforhold = Oppdragsforhold(standardPeriode)
//    private val standardFrilanser: Oppdragsforhold = Oppdragsforhold(standardPeriode)
//    private val standardArbeid: Arbeid = Arbeid(arbeidstaker = listOf(standardArbeidsgiver), selvstendigNaeringsdrivende = listOf(standardSelvstendigNaeringsdrivende), frilanser = listOf(standardFrilanser))
//    private val standardTilleggsinformasjon: String = "Lorem ipsum dolor sit amet."
//    private val standardBeredskap: List<PeriodeMedTilleggsinformasjon> = listOf(PeriodeMedTilleggsinformasjon(standardTilleggsinformasjon, standardPeriode))
//    private val standardNattevaak: List<PeriodeMedTilleggsinformasjon> = listOf(PeriodeMedTilleggsinformasjon(standardTilleggsinformasjon, standardPeriode))
//    private val standardITilsynsordning: JaNeiVetikke = JaNeiVetikke.ja
//    private val standardTilsynsvarighet: Duration = Duration.ofHours(8)
//    private val standardTilsynsordningspphold: Opphold = Opphold(periode = standardPeriode, mandag = standardTilsynsvarighet, tirsdag = standardTilsynsvarighet, onsdag = standardTilsynsvarighet, torsdag = standardTilsynsvarighet, fredag = standardTilsynsvarighet)
//    private val standardTilsynsordning: Tilsynsordning = Tilsynsordning(iTilsynsordning = standardITilsynsordning, opphold = listOf(standardTilsynsordningspphold))
//
//    // Ventede resultatverdier
//    private val konvertertDato: ZonedDateTime = standardDatoMottatt.atStartOfDay(ZoneId.systemDefault())
//    private val konvertertPeriode: no.nav.k9.søknad.felles.Periode = no.nav.k9.søknad.felles.Periode.builder().fraOgMed(standardFraOgMed).tilOgMed(standardTilOgMed).build()
//    private val konvertertSpraak: no.nav.k9.søknad.felles.Språk = no.nav.k9.søknad.felles.Språk.NORSK_BOKMÅL
//    private val konvertertITilsynsordning: TilsynsordningSvar = TilsynsordningSvar.JA
//    private val konvertertGrad = standardGrad.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
//
//    fun genererSoknad(
//            datoMottatt: LocalDate? = standardDatoMottatt,
//            perioder: List<Periode>? = standardPerioder,
//            spraak: Språk? = standardSpraak,
//            barn: Barn? = standardBarn,
//            beredskap: List<PeriodeMedTilleggsinformasjon>? = standardBeredskap,
//            nattevaak: List<PeriodeMedTilleggsinformasjon>? = standardNattevaak,
//            tilsynsordning: Tilsynsordning? = standardTilsynsordning,
//            arbeid: Arbeid? = standardArbeid
//    ): PleiepengerSyktBarnSoknad {
//        return PleiepengerSyktBarnSoknad(
//                datoMottatt = datoMottatt,
//                perioder = perioder,
//                spraak = spraak,
//                barn = barn,
//                beredskap = beredskap,
//                nattevaak = nattevaak,
//                tilsynsordning = tilsynsordning,
//                arbeid = arbeid
//        )
//    }
//
//    @Test
//    fun `Konverterer søknad til riktig format`() {
//
//        val pleiepengerSyktBarnSoknadConverter = PleiepengerSyktBarnYtelseMapper()
//
//        val soknad = genererSoknad()
//
//        val konvertertSoknad = pleiepengerSyktBarnSoknadConverter.mapTil(soknad, standardIdent)
//
//        assertEquals(konvertertDato, konvertertSoknad.mottattDato)
//        assert(konvertertSoknad.perioder.containsKey(konvertertPeriode))
//        assertEquals(konvertertSpraak, konvertertSoknad.språk)
//        assertEquals(standardBarnetsIdent, konvertertSoknad.barn.norskIdentitetsnummer.toString())
//        assertNull(konvertertSoknad.barn.fødselsdato)
//        assertEquals(standardOrganisasjonsnummer, konvertertSoknad.arbeid.arbeidstaker[0].organisasjonsnummer.verdi)
//        assert(konvertertSoknad.arbeid.arbeidstaker[0].perioder.containsKey(konvertertPeriode))
//        assertEquals(konvertertGrad, konvertertSoknad.arbeid.arbeidstaker[0].perioder[konvertertPeriode]!!.skalJobbeProsent)
//        assert(konvertertSoknad.beredskap.perioder.containsKey(konvertertPeriode))
//        assertEquals(standardTilleggsinformasjon, konvertertSoknad.beredskap.perioder[konvertertPeriode]!!.tilleggsinformasjon)
//        assert(konvertertSoknad.nattevåk.perioder.containsKey(konvertertPeriode))
//        assertEquals(standardTilleggsinformasjon, konvertertSoknad.nattevåk.perioder[konvertertPeriode]!!.tilleggsinformasjon)
//        assertEquals(konvertertITilsynsordning, konvertertSoknad.tilsynsordning.iTilsynsordning)
//
//        // TODO: TSF-1185: testfeil: 22 != 5
//        assertEquals(22, konvertertSoknad.tilsynsordning.opphold.size)
//    }
//
//    @Test
//    fun `Konverterer tilsynsordningsopphold til riktig format når det er oppgitt flere tilsynsperioder`() {
//
//        val pleiepengerSyktBarnSoknadConverter = PleiepengerSyktBarnYtelseMapper()
//
//        val fraOgMed1: LocalDate = LocalDate.of(2020, 3, 2)
//        val tilOgMed1: LocalDate = LocalDate.of(2020, 3, 6)
//        val fraOgMed2: LocalDate = LocalDate.of(2020, 3, 9)
//        val tilOgMed2: LocalDate = LocalDate.of(2020, 3, 20)
//        val fraOgMed3: LocalDate = LocalDate.of(2020, 3, 25)
//        val tilOgMed3: LocalDate = LocalDate.of(2020, 3, 25)
//
//        val periode1 = Periode(fraOgMed1, tilOgMed1)
//        val periode2 = Periode(fraOgMed2, tilOgMed2)
//        val periode3 = Periode(fraOgMed3, tilOgMed3)
//
//        val periode1Mandag = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 2)).build()
//        val periode1Tirsdag = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 3)).build()
//        val periode1Onsdag = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 4)).build()
//        val periode1Torsdag = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 5)).build()
//        val periode1Fredag = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 6)).build()
//        val periode2Mandag1 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 9)).build()
//        val periode2Tirsdag1 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 10)).build()
//        val periode2Onsdag1 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 11)).build()
//        val periode2Torsdag1 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 12)).build()
//        val periode2Fredag1 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 13)).build()
//        val periode2Mandag2 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 16)).build()
//        val periode2Tirsdag2 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 17)).build()
//        val periode2Onsdag2 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 18)).build()
//        val periode2Torsdag2 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 19)).build()
//        val periode2Fredag2 = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 20)).build()
//        val periode3Onsdag = no.nav.k9.søknad.felles.Periode.builder().enkeltDag(LocalDate.of(2020, 3, 25)).build()
//
//        val varighet1Mandag = Duration.ofHours(1)
//        val varighet1Tirsdag = Duration.ofHours(2)
//        val varighet1Onsdag = Duration.ofHours(3)
//        val varighet1Torsdag = Duration.ofHours(4)
//        val varighet1Fredag = Duration.ofHours(5)
//        val varighet2Mandag = Duration.ofHours(6)
//        val varighet2Tirsdag = Duration.ofHours(7)
//        val varighet2Onsdag = Duration.ofHours(8)
//        val varighet2Torsdag = Duration.ofMinutes(30)
//        val varighet2Fredag = Duration.ofMinutes(90)
//        val varighet3Onsdag = Duration.ofMinutes(150)
//
//        val opphold1 = Opphold(periode1, varighet1Mandag, varighet1Tirsdag, varighet1Onsdag, varighet1Torsdag, varighet1Fredag)
//        val opphold2 = Opphold(periode2, varighet2Mandag, varighet2Tirsdag, varighet2Onsdag, varighet2Torsdag, varighet2Fredag)
//        val opphold3 = Opphold(periode3, null, null, varighet3Onsdag, null, null)
//
//        val tilsynsordning = Tilsynsordning(standardITilsynsordning, listOf(opphold1, opphold2, opphold3))
//
//        val soknad: PleiepengerSyktBarnSoknad = genererSoknad(tilsynsordning = tilsynsordning)
//        val konvertertSoknad = pleiepengerSyktBarnSoknadConverter.mapTil(soknad, standardIdent)
//        val konverterteOpphold = konvertertSoknad.tilsynsordning.opphold;
//
//        assert(konverterteOpphold.containsKey(periode1Mandag)) // TODO: TSF-1185: feiler
//        assert(konverterteOpphold.containsKey(periode1Tirsdag))
//        assert(konverterteOpphold.containsKey(periode1Onsdag))
//        assert(konverterteOpphold.containsKey(periode1Torsdag))
//        assert(konverterteOpphold.containsKey(periode1Fredag))
//        assert(konverterteOpphold.containsKey(periode2Mandag1))
//        assert(konverterteOpphold.containsKey(periode2Tirsdag1))
//        assert(konverterteOpphold.containsKey(periode2Onsdag1))
//        assert(konverterteOpphold.containsKey(periode2Torsdag1))
//        assert(konverterteOpphold.containsKey(periode2Fredag1))
//        assert(konverterteOpphold.containsKey(periode2Mandag2))
//        assert(konverterteOpphold.containsKey(periode2Tirsdag2))
//        assert(konverterteOpphold.containsKey(periode2Onsdag2))
//        assert(konverterteOpphold.containsKey(periode2Torsdag2))
//        assert(konverterteOpphold.containsKey(periode2Fredag2))
//        assert(konverterteOpphold.containsKey(periode3Onsdag))
//
//        assertEquals(varighet1Mandag, konverterteOpphold[periode1Mandag]!!.lengde)
//        assertEquals(varighet1Tirsdag, konverterteOpphold[periode1Tirsdag]!!.lengde)
//        assertEquals(varighet1Onsdag, konverterteOpphold[periode1Onsdag]!!.lengde)
//        assertEquals(varighet1Torsdag, konverterteOpphold[periode1Torsdag]!!.lengde)
//        assertEquals(varighet1Fredag, konverterteOpphold[periode1Fredag]!!.lengde)
//        assertEquals(varighet2Mandag, konverterteOpphold[periode2Mandag1]!!.lengde)
//        assertEquals(varighet2Mandag, konverterteOpphold[periode2Mandag2]!!.lengde)
//        assertEquals(varighet2Tirsdag, konverterteOpphold[periode2Tirsdag1]!!.lengde)
//        assertEquals(varighet2Tirsdag, konverterteOpphold[periode2Tirsdag2]!!.lengde)
//        assertEquals(varighet2Onsdag, konverterteOpphold[periode2Onsdag1]!!.lengde)
//        assertEquals(varighet2Onsdag, konverterteOpphold[periode2Onsdag2]!!.lengde)
//        assertEquals(varighet2Torsdag, konverterteOpphold[periode2Torsdag1]!!.lengde)
//        assertEquals(varighet2Torsdag, konverterteOpphold[periode2Torsdag2]!!.lengde)
//        assertEquals(varighet2Fredag, konverterteOpphold[periode2Fredag1]!!.lengde)
//        assertEquals(varighet2Fredag, konverterteOpphold[periode2Fredag2]!!.lengde)
//        assertEquals(varighet3Onsdag, konverterteOpphold[periode3Onsdag]!!.lengde)
//
//        assertEquals(16, konverterteOpphold.size)
//    }
}
