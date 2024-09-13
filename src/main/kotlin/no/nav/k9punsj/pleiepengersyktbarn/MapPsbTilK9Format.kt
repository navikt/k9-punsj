package no.nav.k9punsj.pleiepengersyktbarn

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.Kildesystem
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.*
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning
import no.nav.k9punsj.felles.ZoneUtils.Oslo
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somDuration
import no.nav.k9punsj.felles.k9format.*
import no.nav.k9punsj.felles.k9format.MappingUtils.mapEllerLeggTilFeil
import no.nav.k9punsj.utils.PeriodeUtils.erSatt
import no.nav.k9punsj.utils.PeriodeUtils.jsonPath
import no.nav.k9punsj.utils.PeriodeUtils.somK9Periode
import no.nav.k9punsj.utils.PeriodeUtils.somK9Perioder
import no.nav.k9punsj.utils.StringUtils.blankAsNull
import no.nav.k9punsj.utils.StringUtils.erSatt
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZonedDateTime

internal class MapPsbTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    perioderSomFinnesIK9: List<PeriodeDto>,
    dto: PleiepengerSyktBarnSøknadDto
) {

    private val søknad = Søknad().medKildesystem(Kildesystem.PUNSJ)
    private val pleiepengerSyktBarn = PleiepengerSyktBarn()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            søknadId.leggTilSøknadId()
            Versjon.leggTilVersjon()
            dto.leggTilMottattDatoOgKlokkeslett()
            dto.soekerId?.leggTilSøker()
            dto.leggTilJournalposter(journalpostIder = journalpostIder)
            dto.barn?.leggTilBarn()
            dto.soeknadsperiode?.leggTilSøknadsperiode()
            dto.trekkKravPerioder.leggTilTrekkKravPerioder()
            dto.uttak.leggTilUttak(søknadsperiode = dto.soeknadsperiode)
            dto.leggTilLovestemtFerie()
            dto.beredskap?.leggTilBeredskap()
            dto.nattevaak?.leggTilNattevåk()
            dto.bosteder?.mapTilBosteder()?.apply {
                pleiepengerSyktBarn.medBosteder(this)
            }
            if (dto.utenlandsoppholdV2.isNotEmpty()) {
                dto.utenlandsoppholdV2.leggTilUtenlandsoppholdV2(feil).apply {
                    pleiepengerSyktBarn.medUtenlandsopphold(this)
                }
            } else {
                dto.utenlandsopphold?.leggTilUtenlandsopphold(feil)?.apply {
                    pleiepengerSyktBarn.medUtenlandsopphold(this)
                }
            }
            dto.omsorg?.leggTilOmsorg()
            // Sletter oppgitt opptjening dersom søknadsperiode eller arbeidstid er satt OG oppgitt opptjening ikkje er satt
            // Ignorerer oppgitt opptjening dersom ingen søknadsperiode eller arbeidstid er satt
            if (!dto.soeknadsperiode.isNullOrEmpty() || dto.arbeidstid != null || erOpptjeningSatt(dto, dto.opptjeningAktivitet)) {
                dto.opptjeningAktivitet?.mapOpptjeningAktivitet(feil)?.apply {
                    pleiepengerSyktBarn.medOpptjeningAktivitet(this)
                }
            } else {
                pleiepengerSyktBarn.ignorerOpplysningerOmOpptjening()
            }
            dto.arbeidstid?.mapTilArbeidstid(feil)?.apply {
                pleiepengerSyktBarn.medArbeidstid(this)
            }
            dto.soknadsinfo?.leggTilDataBruktTilUtledning()
            dto.tilsynsordning?.perioder?.leggTilTilsynsordning()
            dto.leggTilBegrunnelseForInnsending()

            // Fullfører søknad & validerer
            søknad.medYtelse(pleiepengerSyktBarn)
            feil.addAll(Validator.valider(søknad, perioderSomFinnesIK9.somK9Perioder()))
        }.onFailure { throwable ->
            logger.warn("Uventet mappingfeil", throwable)
            feil.add(Feil("søknad", "uventetMappingfeil", throwable.message ?: "Uventet mappingfeil"))
        }
    }

    internal fun søknad() = søknad

    internal fun feil() = feil.toList()
    internal fun søknadOgFeil() = søknad() to feil()
    private fun String.leggTilSøknadId() {
        if (erSatt()) {
            søknad.medSøknadId(this)
        }
    }

    private fun String.leggTilVersjon() {
        søknad.medVersjon(this)
    }

    private fun PleiepengerSyktBarnSøknadDto.leggTilMottattDatoOgKlokkeslett() {
        if (mottattDato == null) {
            feil.add(Feil("søknad", "mottattDato", "Mottatt dato mangler"))
            return
        }
        if (klokkeslett == null) {
            feil.add(Feil("søknad", "klokkeslett", "Klokkeslett mangler"))
            return
        }

        søknad.medMottattDato(ZonedDateTime.of(mottattDato, klokkeslett, Oslo))
    }

    private fun List<PeriodeDto>.leggTilSøknadsperiode() {
        if (this.isNotEmpty()) {
            pleiepengerSyktBarn.medSøknadsperiode(this.somK9Perioder())
        }
    }

    private fun PleiepengerSyktBarnSøknadDto.BarnDto.leggTilBarn() {
        val barn = Barn()
        when {
            norskIdent.erSatt() -> barn.medNorskIdentitetsnummer(NorskIdentitetsnummer.of(norskIdent))
            foedselsdato != null -> barn.medFødselsdato(foedselsdato)
        }
        pleiepengerSyktBarn.medBarn(barn)
    }

    private fun String.leggTilSøker() {
        if (erSatt()) {
            søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
        }
    }

    private fun List<PleiepengerSyktBarnSøknadDto.NattevåkDto>.leggTilNattevåk() {
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

    private fun List<PleiepengerSyktBarnSøknadDto.BeredskapDto>.leggTilBeredskap() {
        val k9Beredskap = mutableMapOf<Periode, Beredskap.BeredskapPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { beredskap ->
            val k9Periode = beredskap.periode!!.somK9Periode()!!
            val k9Info = Beredskap.BeredskapPeriodeInfo()
            if (beredskap.tilleggsinformasjon.erSatt()) {
                k9Info.medTilleggsinformasjon(beredskap.tilleggsinformasjon)
            }
            k9Beredskap[k9Periode] = k9Info
        }
        if (k9Beredskap.isNotEmpty()) {
            pleiepengerSyktBarn.medBeredskap(Beredskap().medPerioder(k9Beredskap))
        }
    }

    private fun List<PleiepengerSyktBarnSøknadDto.UttakDto>?.leggTilUttak(søknadsperiode: List<PeriodeDto>?) {
        val k9Uttak = mutableMapOf<Periode, Uttak.UttakPeriodeInfo>()

        this?.filter { it.periode.erSatt() }?.forEach { uttak ->
            val k9Periode = uttak.periode!!.somK9Periode()!!
            val k9Info = Uttak.UttakPeriodeInfo()
            mapEllerLeggTilFeil(
                feil = feil,
                felt = "ytelse.uttak.perioder.${k9Periode.jsonPath()}.timerPleieAvBarnetPerDag"
            ) { uttak.pleieAvBarnetPerDag?.somDuration() }?.also {
                k9Info.medTimerPleieAvBarnetPerDag(it)
            }
            k9Uttak[k9Periode] = k9Info
        }

        if (k9Uttak.isEmpty() && søknadsperiode != null) {
            søknadsperiode.forEach { periode ->
                periode.somK9Periode()?.let { k9Uttak[it] = DefaultUttak }
            }
        }

        if (k9Uttak.isNotEmpty()) {
            pleiepengerSyktBarn.medUttak(Uttak().medPerioder(k9Uttak))
        }
    }

    private fun PleiepengerSyktBarnSøknadDto.leggTilLovestemtFerie() {
        if (lovbestemtFerie.isNullOrEmpty() && lovbestemtFerieSomSkalSlettes.isNullOrEmpty()) {
            return
        }
        val k9LovbestemtFerie = mutableMapOf<Periode, LovbestemtFerie.LovbestemtFeriePeriodeInfo>()
        lovbestemtFerie?.filter { it.erSatt() }?.forEach { periode ->
            k9LovbestemtFerie[periode.somK9Periode()!!] =
                LovbestemtFerie.LovbestemtFeriePeriodeInfo().medSkalHaFerie(true)
        }
        lovbestemtFerieSomSkalSlettes?.filter { it.erSatt() }?.forEach { periode ->
            k9LovbestemtFerie[periode.somK9Periode()!!] =
                LovbestemtFerie.LovbestemtFeriePeriodeInfo().medSkalHaFerie(false)
        }
        pleiepengerSyktBarn.medLovbestemtFerie(LovbestemtFerie().medPerioder(k9LovbestemtFerie))
    }

    private fun List<PeriodeDto>.leggTilTrekkKravPerioder() {
        pleiepengerSyktBarn.addAllTrekkKravPerioder(this.somK9Perioder())
    }

    private fun PleiepengerSyktBarnSøknadDto.leggTilBegrunnelseForInnsending() {
        if (begrunnelseForInnsending != null) {
            søknad.medBegrunnelseForInnsending(begrunnelseForInnsending)
        }
    }

    private fun PleiepengerSyktBarnSøknadDto.leggTilJournalposter(journalpostIder: Set<String>) {
        journalpostIder.forEach { journalpostId ->
            søknad.medJournalpost(
                Journalpost()
                    .medJournalpostId(journalpostId)
                    .medInformasjonSomIkkeKanPunsjes(harInfoSomIkkeKanPunsjes)
                    .medInneholderMedisinskeOpplysninger(harMedisinskeOpplysninger)
            )
        }
    }

    private fun PleiepengerSyktBarnSøknadDto.OmsorgDto.leggTilOmsorg() {
        val k9Omsorg = Omsorg()
        mapEllerLeggTilFeil(feil, "ytelse.omsorg.relasjonTilBarnet") {
            relasjonTilBarnet?.blankAsNull()?.let { Omsorg.BarnRelasjon.valueOf(it.uppercase()) }
        }?.also { k9Omsorg.medRelasjonTilBarnet(it) }

        if (beskrivelseAvOmsorgsrollen.erSatt()) {
            k9Omsorg.medBeskrivelseAvOmsorgsrollen(beskrivelseAvOmsorgsrollen!!)
        }
        pleiepengerSyktBarn.medOmsorg(k9Omsorg)
    }

    private fun PleiepengerSyktBarnSøknadDto.DataBruktTilUtledningDto.leggTilDataBruktTilUtledning() {
        val k9DataBruktTilUtledning = DataBruktTilUtledning()
        samtidigHjemme?.also { k9DataBruktTilUtledning.medSamtidigHjemme(it) }
        harMedsoeker?.also { k9DataBruktTilUtledning.medHarMedsøker(it) }
        pleiepengerSyktBarn.medSøknadInfo(k9DataBruktTilUtledning)
    }

    private fun List<PleiepengerSyktBarnSøknadDto.TilsynsordningInfoDto>.leggTilTilsynsordning() {
        val k9Tilsynsordning = mutableMapOf<Periode, TilsynPeriodeInfo>()
        filter { it.periode.erSatt() }.forEach { tilsynsordning ->
            val k9Periode = tilsynsordning.periode!!.somK9Periode()!!
            k9Tilsynsordning[k9Periode] = TilsynPeriodeInfo()
                .medEtablertTilsynTimerPerDag(tilsynsordning.somDuration())
        }
        if (k9Tilsynsordning.isNotEmpty()) {
            pleiepengerSyktBarn.medTilsynsordning(Tilsynsordning().medPerioder(k9Tilsynsordning))
        }
    }

    private fun erOpptjeningSatt(
        dto: PleiepengerSyktBarnSøknadDto,
        opptjeningAktivitet: ArbeidAktivitetDto?
    ) = dto.opptjeningAktivitet != null &&
        (
            !opptjeningAktivitet?.arbeidstaker.isNullOrEmpty() ||
                opptjeningAktivitet?.frilanser != null ||
                opptjeningAktivitet?.selvstendigNaeringsdrivende != null
            )

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapPsbTilK9Format::class.java)

        private val Validator = PleiepengerSyktBarnSøknadValidator()
        private const val Versjon = "1.0.0"

        private val DefaultUttak =
            Uttak.UttakPeriodeInfo().medTimerPleieAvBarnetPerDag(Duration.ofHours(7).plusMinutes(30))
    }
}
