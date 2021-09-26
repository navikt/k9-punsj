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
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

internal class PleiepengerSyktBarnMapper(
    søknadId: String,
    journalpostIder: Set<String>,
    perioderSomFinnesIK9: List<PeriodeDto>,
    dto: PleiepengerSøknadVisningDto) {
    private val søknad = Søknad()
    private val pleiepengerSyktBarn = PleiepengerSyktBarn()
    private val feil = mutableListOf<Feil>()

    init {
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
    }

    internal val innheholderFeil = feil.isNotEmpty()
    internal val inneholderGyldigSøknad = feil.isEmpty()
    internal fun søknad() = søknad
    internal fun feil() = feil.toList()
    internal fun søknadOgFeil() = søknad() to feil()

    private fun <Til>mapEllerLeggTilFeil(felt: String, map: () -> Til?) = kotlin.runCatching {
        map()
    }.fold(onSuccess = {it}, onFailure = {
        feil.add(Feil(felt, felt, it.message ?: "Ingen feilmelding"))
        null
    })

    private fun String.leggTilSøknadId() {
        if (!erSatt()) return
        søknad.medSøknadId(this)
    }

    private fun String.leggTilVersjon() {
        søknad.medVersjon(this)
    }

    private fun PleiepengerSøknadVisningDto.leggTilMottattDato() {
        if (mottattDato == null || klokkeslett == null) return
        val mottatt = ZonedDateTime.of(mottattDato, klokkeslett, Oslo)
        søknad.medMottattDato(mottatt)
    }

    private fun PeriodeDto.leggTilSøknadsperiode() {
        if (!erSatt()) return
        pleiepengerSyktBarn.medSøknadsperiode(somK9Periode()!!)
    }

    private fun PleiepengerSøknadVisningDto.BarnDto.leggTilBarn() {
        when {
            norskIdent.erSatt() -> Barn.builder().norskIdentitetsnummer(NorskIdentitetsnummer.of(norskIdent)).build()
            foedselsdato != null -> Barn.builder().fødselsdato(foedselsdato).build()
            else -> null
        }?.also { pleiepengerSyktBarn.medBarn(it) }
    }

    private fun String.leggTilSøker() {
        if (!erSatt()) return
        søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
    }

    private fun List<PleiepengerSøknadVisningDto.BostederDto>.leggTilBosteder() {
        val k9Bosteder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { bosted ->
            k9Bosteder[bosted.periode!!.somK9Periode()!!] = Bosteder.BostedPeriodeInfo()
                .let { if (bosted.land.erSatt()) it.medLand(Landkode.of(bosted.land)) else it }
        }
        pleiepengerSyktBarn.medBosteder(Bosteder().medPerioder(k9Bosteder))
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
                mapEllerLeggTilFeil("utenlandsopphold.$k9Periode.årsak") {
                    Utenlandsopphold.UtenlandsoppholdÅrsak.valueOf(utenlandsopphold.årsak!!)
                }?.also { k9Info.medÅrsak(it) }
            }

            k9Utenlandsopphold[k9Periode] = k9Info
        }
        pleiepengerSyktBarn.medUtenlandsopphold(Utenlandsopphold().medPerioder(k9Utenlandsopphold))
    }

    private fun List<PleiepengerSøknadVisningDto.NattevåkDto>.leggTilNattevåk() {
        val k9Nattevåk = mutableMapOf<Periode, Nattevåk.NattevåkPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { nattevåk ->
            k9Nattevåk[nattevåk.periode!!.somK9Periode()!!] = Nattevåk.NattevåkPeriodeInfo()
                .let { if (nattevåk.tilleggsinformasjon.erSatt()) it.medTilleggsinformasjon(nattevåk.tilleggsinformasjon) else it }
        }
        pleiepengerSyktBarn.medNattevåk(Nattevåk().medPerioder(k9Nattevåk))
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
            mapEllerLeggTilFeil("uttak.$periode.timerPleieAvBarnetPerDag") { uttak.timerPleieAvBarnetPerDag?.let { Duration.parse(it) } }?.let { periodeInfo.setTimerPleieAvBarnetPerDag(it) }
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
            relasjonTilBarnet?.let { Omsorg.BarnRelasjon.valueOf(it) }
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

    private fun PleiepengerSøknadVisningDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.mapOpptjeningAktivitetSelvstendigNæringsdrivende() : SelvstendigNæringsdrivende {
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
            if (!info.virksomhetstyper.isNullOrEmpty()) {
                val k9Virksomhetstyper = info.virksomhetstyper.mapIndexed { index, virksomhetstype -> mapEllerLeggTilFeil("opptjening.selvstendigNæringsdrivende.$k9Periode.virksomhetstyper[$index]") {
                    VirksomhetType.valueOf(virksomhetstype)
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
        // TODO: Mappe Arbeidstid
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

    private companion object {
        private val Oslo = ZoneId.of("Europe/Oslo")
        private val Validator = PleiepengerSyktBarnSøknadValidator()
        private val Versjon = "1.0.0"
        private val DefaultUttak = UttakPeriodeInfo().setTimerPleieAvBarnetPerDag(Duration.ofHours(7).plusMinutes(30))
        private fun PeriodeDto?.erSatt() = this != null && (fom != null || tom != null)
        private fun PeriodeDto.somK9Periode() = when (erSatt()) {
            true -> Periode(fom, tom)
            else -> null
        }
        private fun Collection<PeriodeDto>.somK9Perioder() = mapNotNull { it.somK9Periode() }
        private fun String?.erSatt() = !isNullOrBlank()
    }
}

