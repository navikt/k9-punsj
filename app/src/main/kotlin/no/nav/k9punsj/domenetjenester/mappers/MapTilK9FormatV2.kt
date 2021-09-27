package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.opptjening.Arbeidstaker
import no.nav.k9.søknad.felles.opptjening.Frilanser
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet
import no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.*
import no.nav.k9.søknad.ytelse.psb.v1.*
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToLong

internal class MapTilK9FormatV2(
    søknadId: String,
    journalpostIder: Set<String>,
    perioderSomFinnesIK9: List<PeriodeDto>,
    dto: PleiepengerSøknadVisningDto) {
    private val søknad = Søknad()
    private val pleiepengerSyktBarn = PleiepengerSyktBarn()
    private val feil = mutableListOf<Feil>()

    init { kotlin.runCatching {
        søknadId.leggTilSøknadId()
        Versjon.leggTilVersjon()
        dto.leggTilMottattDato()
        dto.soekerId?.leggTilSøker()
        dto.leggTilJournalposter(journalpostIder = journalpostIder)
        dto.barn?.leggTilBarn()
        dto.soeknadsperiode?.leggTilSøknadsperiode()
        perioderSomFinnesIK9.leggTilEndringsperioder() // TODO: Fjernes når vi endrer til k9-format som utleder endringsperioder.
        dto.trekkKravPerioder.leggTilTrekkKravPerioder()
        dto.uttak.leggTilUttak(søknadsperiode = dto.soeknadsperiode)
        dto.leggTilLovestemtFerie()
        dto.beredskap?.leggTilBeredskap()
        dto.nattevaak?.leggTilNattevåk()
        dto.bosteder?.leggTilBosteder()
        dto.utenlandsopphold?.leggTilUtenlandsopphold()
        dto.omsorg?.leggTilOmsorg()
        dto.opptjeningAktivitet?.leggTilOpptjeningAktivitet()
        dto.arbeidstid?.leggTilArbeidstid()
        dto.soknadsinfo?.leggTilDataBruktTilUtledning()
        dto.tilsynsordning?.perioder?.leggTilTilsynsordning()

        // Fullfører søknad & validerer
        søknad.medYtelse(pleiepengerSyktBarn)
        feil.addAll(Validator.valider(søknad)) //TODO: Send med perioderSomFinnesIK9 når vi endrer til k9-format som utleder endringsperioder.
    }.onFailure { throwable ->
        logger.error("Uventet mappingfeil", throwable)
        feil.add(Feil("søknad", "uventetMappingfeil", throwable.message ?: "Uventet mappingfeil"))
    }}

    internal fun søknad() = søknad
    internal fun feil() = feil.toList()
    internal fun søknadOgFeil() = søknad() to feil()

    private fun String.leggTilSøknadId() { if (erSatt()) {
        søknad.medSøknadId(this)
    }}

    private fun String.leggTilVersjon() {
        søknad.medVersjon(this)
    }

    private fun PleiepengerSøknadVisningDto.leggTilMottattDato() { if (mottattDato != null && klokkeslett != null) {
        søknad.medMottattDato(ZonedDateTime.of(mottattDato, klokkeslett, Oslo))
    }}

    private fun PeriodeDto.leggTilSøknadsperiode() { if (erSatt()) {
        pleiepengerSyktBarn.medSøknadsperiode(somK9Periode()!!)
    }}

    private fun PleiepengerSøknadVisningDto.BarnDto.leggTilBarn() {
        val barn = when {
            norskIdent.erSatt() -> Barn.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(norskIdent)).build()
            foedselsdato != null -> Barn.builder().fødselsdato(foedselsdato).build()
            else -> Barn.builder().build()
        }
        pleiepengerSyktBarn.medBarn(barn)
    }

    private fun String.leggTilSøker() { if (erSatt()) {
        søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
    }}

    private fun List<PleiepengerSøknadVisningDto.BostederDto>.leggTilBosteder() {
        val k9Bosteder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { bosted ->
            val k9Periode = bosted.periode!!.somK9Periode()!!
            val k9Info = Bosteder.BostedPeriodeInfo()
            if (bosted.land.erSatt()) {
                k9Info.medLand(Landkode.of(bosted.land))
            }
            k9Bosteder[k9Periode] = k9Info
        }
        if (k9Bosteder.isNotEmpty()) {
            pleiepengerSyktBarn.medBosteder(Bosteder().medPerioder(k9Bosteder))
        }
    }

    private fun List<PleiepengerSøknadVisningDto.UtenlandsoppholdDto>.leggTilUtenlandsopphold() {
        val k9Utenlandsopphold = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { utenlandsopphold ->
            val k9Periode = utenlandsopphold.periode!!.somK9Periode()!!
            val k9Info = Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
            if (utenlandsopphold.land.erSatt()) {
                k9Info.medLand(Landkode.of(utenlandsopphold.land))
            }
            if (utenlandsopphold.årsak.erSatt()) {
                mapEllerLeggTilFeil("ytelse.utenlandsopphold.$k9Periode.årsak") {
                    Utenlandsopphold.UtenlandsoppholdÅrsak.valueOf(utenlandsopphold.årsak!!)
                }?.also { k9Info.medÅrsak(it) }
            }

            k9Utenlandsopphold[k9Periode] = k9Info
        }
        if (k9Utenlandsopphold.isNotEmpty()) {
            pleiepengerSyktBarn.medUtenlandsopphold(Utenlandsopphold().medPerioder(k9Utenlandsopphold))
        }
    }

    private fun List<PleiepengerSøknadVisningDto.NattevåkDto>.leggTilNattevåk() {
        val k9Nattevåk = mutableMapOf<Periode, Nattevåk.NattevåkPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { nattevåk ->
            val k9Periode = nattevåk.periode!!.somK9Periode()!!
            val k9Info = Nattevåk.NattevåkPeriodeInfo()
            if (nattevåk.tilleggsinformasjon.erSatt()) {
                k9Info.medTilleggsinformasjon(nattevåk.tilleggsinformasjon)
            }
            k9Nattevåk[k9Periode] = k9Info
        }
        if (k9Nattevåk.isNotEmpty()) {
            pleiepengerSyktBarn.medNattevåk(Nattevåk().medPerioder(k9Nattevåk))
        }
    }

    private fun List<PleiepengerSøknadVisningDto.BeredskapDto>.leggTilBeredskap() {
        val k9Beredskap = mutableMapOf<Periode, Beredskap.BeredskapPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { beredskap ->
            k9Beredskap[beredskap.periode!!.somK9Periode()!!] = Beredskap.BeredskapPeriodeInfo()
                .let { if (beredskap.tilleggsinformasjon.erSatt()) it.medTilleggsinformasjon(beredskap.tilleggsinformasjon) else it }
        }
        pleiepengerSyktBarn.medBeredskap(Beredskap().medPerioder(k9Beredskap))
    }

    private fun List<PleiepengerSøknadVisningDto.UttakDto>?.leggTilUttak(søknadsperiode: PeriodeDto?) {
        val k9Uttak = mutableMapOf<Periode, UttakPeriodeInfo>()
        this?.filter { it.periode.erSatt() }?.forEach { uttak ->
            val periode = uttak.periode!!.somK9Periode()!!
            val periodeInfo = UttakPeriodeInfo()
            mapEllerLeggTilFeil("uttak.$periode.timerPleieAvBarnetPerDag") { uttak.timerPleieAvBarnetPerDag?.somDuration() }?.also {
                periodeInfo.timerPleieAvBarnetPerDag = it
            }
            k9Uttak[periode] = periodeInfo
        }

        if (k9Uttak.isEmpty() && søknadsperiode.erSatt()) {
            k9Uttak[søknadsperiode!!.somK9Periode()!!] = DefaultUttak
        }

        pleiepengerSyktBarn.medUttak(Uttak().medPerioder(k9Uttak))
    }

    private fun PleiepengerSøknadVisningDto.leggTilLovestemtFerie() {
        if (lovbestemtFerie.isNullOrEmpty() && lovbestemtFerieSomSkalSlettes.isNullOrEmpty()) {
            return
        }
        val k9LovbestemtFerie = mutableMapOf<Periode, LovbestemtFerie.LovbestemtFeriePeriodeInfo>()
        lovbestemtFerie?.filter { it.erSatt() }?.forEach { periode ->
            k9LovbestemtFerie[periode.somK9Periode()!!] = LovbestemtFerie.LovbestemtFeriePeriodeInfo().medSkalHaFerie(true)
        }
        lovbestemtFerieSomSkalSlettes?.filter { it.erSatt() }?.forEach { periode ->
            k9LovbestemtFerie[periode.somK9Periode()!!] = LovbestemtFerie.LovbestemtFeriePeriodeInfo().medSkalHaFerie(false)
        }
        pleiepengerSyktBarn.medLovbestemtFerie(LovbestemtFerie().medPerioder(k9LovbestemtFerie))
    }

    private fun List<PeriodeDto>.leggTilEndringsperioder() {
        pleiepengerSyktBarn.medEndringsperiode(this.somK9Perioder())
    }

    private fun Set<PeriodeDto>.leggTilTrekkKravPerioder() {
        pleiepengerSyktBarn.addAllTrekkKravPerioder(this.somK9Perioder())
    }

    private fun PleiepengerSøknadVisningDto.leggTilJournalposter(journalpostIder: Set<JournalpostId>) {
        journalpostIder.forEach { journalpostId ->
            søknad.medJournalpost(Journalpost()
                .medJournalpostId(journalpostId)
                .medInfomasjonSomIkkeKanPunsjes(harInfoSomIkkeKanPunsjes)
                .medInneholderMedisinskeOpplysninger(harMedisinskeOpplysninger)
            )
        }
    }

    private fun PleiepengerSøknadVisningDto.OmsorgDto.leggTilOmsorg() {
        val k9Omsorg = Omsorg()
        mapEllerLeggTilFeil("omsorg.relasjonTilBarnet") {
            relasjonTilBarnet?.let { Omsorg.BarnRelasjon.valueOf(it.uppercase()) }
        }?.also { k9Omsorg.medRelasjonTilBarnet(it) }

        if (beskrivelseAvOmsorgsrollen.erSatt()) {
            k9Omsorg.medBeskrivelseAvOmsorgsrollen(beskrivelseAvOmsorgsrollen!!)
        }

        pleiepengerSyktBarn.medOmsorg(k9Omsorg)
    }

    private fun PleiepengerSøknadVisningDto.ArbeidAktivitetDto.leggTilOpptjeningAktivitet() {
        val k9OpptjeningAktivitet = OpptjeningAktivitet()
        selvstendigNaeringsdrivende?.also { k9OpptjeningAktivitet.medSelvstendigNæringsdrivende(it.mapOpptjeningAktivitetSelvstendigNæringsdrivende()) }
        frilanser?.also { k9OpptjeningAktivitet.medFrilanser(it.mapOpptjeningAktivitetFrilanser()) }
        arbeidstaker?.also { k9OpptjeningAktivitet.medArbeidstaker(it.mapOpptjeningAktivitetArbeidstaker())}
        pleiepengerSyktBarn.medOpptjeningAktivitet(k9OpptjeningAktivitet)
    }

    private fun PleiepengerSøknadVisningDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.mapOpptjeningAktivitetSelvstendigNæringsdrivende() : SelvstendigNæringsdrivende? {
        val k9SelvstendigNæringsdrivende = SelvstendigNæringsdrivende.builder()
        if (organisasjonsnummer.erSatt()) k9SelvstendigNæringsdrivende.organisasjonsnummer(Organisasjonsnummer.of(organisasjonsnummer))
        if (virksomhetNavn.erSatt()) k9SelvstendigNæringsdrivende.virksomhetNavn(virksomhetNavn)
        if (info?.periode.erSatt()) {
            val k9Periode = info!!.periode!!.somK9Periode()
            val k9Info = SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.builder()
            if (info.erNyoppstartet != null) k9Info.erNyoppstartet(info.erNyoppstartet)
            if (info.erVarigEndring != null) k9Info.erVarigEndring(info.erVarigEndring)
            if (info.registrertIUtlandet != null) k9Info.registrertIUtlandet(info.registrertIUtlandet)
            if (info.regnskapsførerNavn.erSatt()) k9Info.regnskapsførerNavn(info.regnskapsførerNavn)
            if (info.regnskapsførerTlf.erSatt()) k9Info.regnskapsførerTelefon(info.regnskapsførerTlf)
            // TODO: Mangler felter
            if (!info.virksomhetstyper.isNullOrEmpty()) {
                val k9Virksomhetstyper = info.virksomhetstyper.mapIndexedNotNull { index, virksomhetstype -> when {
                    virksomhetstype.lowercase().contains("dagmamma") -> VirksomhetType.DAGMAMMA
                    virksomhetstype.lowercase().contains("fiske") -> VirksomhetType.FISKE
                    virksomhetstype.lowercase().contains("jordbruk") -> VirksomhetType.JORDBRUK_SKOGBRUK
                    virksomhetstype.lowercase().contains("annen") -> VirksomhetType.ANNEN
                    else -> mapEllerLeggTilFeil("opptjening.selvstendigNæringsdrivende.$k9Periode.virksomhetstyper[$index]") {
                        VirksomhetType.valueOf(virksomhetstype)
                    }
                }}
                k9Info.virksomhetstyper(k9Virksomhetstyper)
            }
        }
        return k9SelvstendigNæringsdrivende.build()
    }

    private fun PleiepengerSøknadVisningDto.ArbeidAktivitetDto.FrilanserDto.mapOpptjeningAktivitetFrilanser() : Frilanser {
        val k9Frilanser = Frilanser()
        if (jobberFortsattSomFrilans != null) {
            k9Frilanser.medJobberFortsattSomFrilans(jobberFortsattSomFrilans)
        }
        if (startdato.erSatt()) mapEllerLeggTilFeil("opptjening.frilanser.startDato") { LocalDate.parse(startdato) }?.also {
            k9Frilanser.medStartDato(it)
        }
        if (startdato.erSatt()) mapEllerLeggTilFeil("opptjening.frilanser.sluttDato") { LocalDate.parse(sluttdato) }?.also {
            k9Frilanser.medSluttDato(it)
        }
        return k9Frilanser
    }

    private fun List<PleiepengerSøknadVisningDto.ArbeidAktivitetDto.ArbeidstakerDto>.mapOpptjeningAktivitetArbeidstaker() = map { arbeidstaker ->
        val k9Arbeidstaker = Arbeidstaker()
        if (arbeidstaker.norskIdent.erSatt()) {
            k9Arbeidstaker.norskIdentitetsnummer = NorskIdentitetsnummer.of(arbeidstaker.norskIdent)
        }
        if (arbeidstaker.organisasjonsnummer.erSatt()) {
            k9Arbeidstaker.organisasjonsnummer = Organisasjonsnummer.of(arbeidstaker.organisasjonsnummer)
        }
        k9Arbeidstaker
    }

    private fun PleiepengerSøknadVisningDto.ArbeidstidDto.leggTilArbeidstid() {
        val k9Arbeidstid = Arbeidstid()
        arbeidstakerList?.also { k9Arbeidstid.medArbeidstaker(it.mapArbeidstidArbeidstaker()) }
        // TODO: Frilanser og selvstendig
        pleiepengerSyktBarn.medArbeidstid(k9Arbeidstid)
    }

    private fun List<PleiepengerSøknadVisningDto.ArbeidAktivitetDto.ArbeidstakerDto>.mapArbeidstidArbeidstaker() = map { arbeidstaker ->
        val k9Arbeidstaker = no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker()
        if (arbeidstaker.norskIdent.erSatt()) {
            k9Arbeidstaker.norskIdentitetsnummer = NorskIdentitetsnummer.of(arbeidstaker.norskIdent)
        }
        if (arbeidstaker.organisasjonsnummer.erSatt()) {
            k9Arbeidstaker.organisasjonsnummer = Organisasjonsnummer.of(arbeidstaker.organisasjonsnummer)
        }
        val k9ArbeidstidPeriodeInfo = mutableMapOf<Periode, ArbeidstidPeriodeInfo>()
        arbeidstaker.arbeidstidInfo?.perioder?.filter { it.periode.erSatt() }?.forEachIndexed{ index, periode ->
            val k9Periode = periode.periode!!.somK9Periode()!!
            val k9Info = ArbeidstidPeriodeInfo()
            val felt = "arbeisdtid.arbeidstakerList[$index].arbeidstidInfo.perioder.$k9Periode"
            if (periode.faktiskArbeidTimerPerDag.erSatt()) mapEllerLeggTilFeil("$felt.faktiskArbeidTimerPerDag") {
                periode.faktiskArbeidTimerPerDag!!.somDuration()
            }?.also { k9Info.medFaktiskArbeidTimerPerDag(it) }
            if (periode.jobberNormaltTimerPerDag.erSatt()) mapEllerLeggTilFeil("$felt.jobberNormaltTimerPerDag") {
                periode.jobberNormaltTimerPerDag!!.somDuration()
            }?.also { k9Info.medJobberNormaltTimerPerDag(it) }
            k9ArbeidstidPeriodeInfo[k9Periode] = k9Info
        }
        k9Arbeidstaker.arbeidstidInfo = ArbeidstidInfo().medPerioder(k9ArbeidstidPeriodeInfo)
        k9Arbeidstaker
    }

    private fun PleiepengerSøknadVisningDto.DataBruktTilUtledningDto.leggTilDataBruktTilUtledning() {
        val k9DataBruktTilUtledning = DataBruktTilUtledning()
        samtidigHjemme?.also { k9DataBruktTilUtledning.medSamtidigHjemme(it) }
        harMedsoeker?.also { k9DataBruktTilUtledning.medHarMedsøker(it) }
        pleiepengerSyktBarn.medSøknadInfo(k9DataBruktTilUtledning)
    }

    private fun List<PleiepengerSøknadVisningDto.TilsynsordningInfoDto>.leggTilTilsynsordning() {
        val k9Tilsynsordning = mutableMapOf<Periode, TilsynPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { tilsynsordning ->
            val k9Periode = tilsynsordning.periode!!.somK9Periode()!!
            k9Tilsynsordning[k9Periode] = TilsynPeriodeInfo()
                .medEtablertTilsynTimerPerDag(Duration
                    .ofHours(tilsynsordning.timer.toLong())
                    .plusMinutes(tilsynsordning.minutter.toLong()
                    )
                )
        }
        pleiepengerSyktBarn.medTilsynsordning(Tilsynsordning().medPerioder(k9Tilsynsordning))
    }

    private fun <Til>mapEllerLeggTilFeil(felt: String, map: () -> Til?) = kotlin.runCatching {
        map()
    }.fold(onSuccess = {it}, onFailure = { throwable ->
        feil.add(Feil(felt, throwable.javaClass.simpleName, throwable.message ?: "Ingen feilmelding"))
        null
    })

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapTilK9FormatV2::class.java)
        private val Oslo = ZoneId.of("Europe/Oslo")
        private val Validator = PleiepengerSyktBarnSøknadValidator()
        private const val Versjon = "1.0.0"
        private val DefaultUttak = UttakPeriodeInfo().setTimerPleieAvBarnetPerDag(Duration.ofHours(7).plusMinutes(30))
        private fun PeriodeDto?.erSatt() = this != null && (fom != null || tom != null)
        private fun PeriodeDto.somK9Periode() = when (erSatt()) {
            true -> Periode(fom, tom)
            else -> null
        }
        private fun Collection<PeriodeDto>.somK9Perioder() = mapNotNull { it.somK9Periode() }
        private fun String?.erSatt() = !isNullOrBlank()
        private fun String.somDesimalOrNull() = replace(",", ".").toDoubleOrNull()
        private val EnTimeInMillis = Duration.ofHours(1).toMillis()
        private fun String.somDuration() : Duration {
            kotlin.runCatching { Duration.parse(this) }.onSuccess { return it }
            if (toLongOrNull() != null) { return Duration.ofHours(toLong())}
            if (somDesimalOrNull() != null) {
                val millis = (somDesimalOrNull()!! * EnTimeInMillis).roundToLong()
                return Duration.ofMillis(millis)
            }
            throw IllegalArgumentException("Ugyldig duration $this")
        }
    }
}