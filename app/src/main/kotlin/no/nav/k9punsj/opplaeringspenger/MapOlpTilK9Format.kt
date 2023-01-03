package no.nav.k9punsj.opplaeringspenger

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.olp.v1.Opplæringspenger
import no.nav.k9.søknad.ytelse.olp.v1.OpplæringspengerSøknadValidator
import no.nav.k9.søknad.ytelse.olp.v1.kurs.Kurs
import no.nav.k9.søknad.ytelse.olp.v1.kurs.KursPeriodeMedReisetid
import no.nav.k9.søknad.ytelse.olp.v1.kurs.Kursholder
import no.nav.k9.søknad.ytelse.psb.v1.LovbestemtFerie
import no.nav.k9.søknad.ytelse.psb.v1.Uttak
import no.nav.k9punsj.felles.ZoneUtils.Oslo
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somDuration
import no.nav.k9punsj.felles.k9format.MappingUtils
import no.nav.k9punsj.felles.k9format.leggTilUtenlandsopphold
import no.nav.k9punsj.felles.k9format.mapOpptjeningAktivitet
import no.nav.k9punsj.felles.k9format.mapTilArbeidstid
import no.nav.k9punsj.utils.PeriodeUtils.erSatt
import no.nav.k9punsj.utils.PeriodeUtils.jsonPath
import no.nav.k9punsj.utils.PeriodeUtils.somK9Periode
import no.nav.k9punsj.utils.PeriodeUtils.somK9Perioder
import no.nav.k9punsj.utils.StringUtils.erSatt
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

internal class MapOlpTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    perioderSomFinnesIK9: List<PeriodeDto>,
    dto: OpplaeringspengerSøknadDto
) {

    private val søknad = Søknad()
    private val opplaeringspenger = Opplæringspenger()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            søknadId.leggTilSøknadId()
            Versjon.leggTilVersjon()
            dto.leggTilMottattDatoOgKlokkeslett()
            dto.soekerId?.leggTilSøker()
            dto.leggTilJournalposter(journalpostIder = journalpostIder)
            dto.barn?.leggTilBarn()
            dto.leggTilLovbestemtFerie()
            dto.soeknadsperiode?.leggTilSøknadsperiode()
            if (dto.utenlandsopphold.isNotEmpty()) {
                dto.utenlandsopphold.leggTilUtenlandsopphold(feil).apply {
                    opplaeringspenger.medUtenlandsopphold(this)
                }
            } else {
                dto.utenlandsopphold?.leggTilUtenlandsopphold(feil)?.apply {
                    opplaeringspenger.medUtenlandsopphold(this)
                }
            }
            dto.arbeidstid?.mapTilArbeidstid(feil)?.apply {
                opplaeringspenger.medArbeidstid(this)
            }
            dto.trekkKravPerioder.leggTilTrekkKravPerioder()
            if (!dto.soeknadsperiode.isNullOrEmpty() || erOpptjeningSatt(dto, dto.opptjeningAktivitet)) {
                dto.opptjeningAktivitet?.mapOpptjeningAktivitet(feil)?.apply {
                    opplaeringspenger.medOpptjeningAktivitet(this)
                }
            }
            dto.leggTilBegrunnelseForInnsending()
            dto.kurs?.leggTilKurs()
            dto.uttak.leggTilUttak(søknadsperiode = dto.soeknadsperiode)

            // Fullfører søknad & validerer
            søknad.medYtelse(opplaeringspenger)
            feil.addAll(Validator.valider(søknad, perioderSomFinnesIK9.somK9Perioder()))
        }.onFailure { throwable ->
            logger.error("Uventet mappingfeil", throwable)
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


    private fun OpplaeringspengerSøknadDto.Kurs.leggTilKurs() {
        val institusjonsUuid = this.kursHolder?.institusjonsUuid?.let { UUID.fromString(it) }
        val kursHolder = Kursholder(this.kursHolder?.holder, institusjonsUuid)
        val kursPerioder = this.kursperioder?.map {
            KursPeriodeMedReisetid(it.periode?.somK9Periode(), it.avreise, it.hjemkomst)
        }?.toMutableList()
        val kurs = Kurs(kursHolder, this.formaal, kursPerioder)
        opplaeringspenger.medKurs(kurs)
    }

    private fun Set<PeriodeDto>.leggTilTrekkKravPerioder() {
        opplaeringspenger.addAllTrekkKravPerioder(this.somK9Perioder())
    }

    private fun OpplaeringspengerSøknadDto.leggTilMottattDatoOgKlokkeslett() {
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
            opplaeringspenger.medSøknadsperiode(this.somK9Perioder())
        }
    }

    private fun OpplaeringspengerSøknadDto.BarnDto.leggTilBarn() {
        val barn = Barn()
        when {
            norskIdent.erSatt() -> barn.medNorskIdentitetsnummer(NorskIdentitetsnummer.of(norskIdent))
            foedselsdato != null -> barn.medFødselsdato(foedselsdato)
        }
        opplaeringspenger.medBarn(barn)
    }

    private fun String.leggTilSøker() {
        if (erSatt()) {
            søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
        }
    }

    private fun OpplaeringspengerSøknadDto.leggTilBegrunnelseForInnsending() {
        if (begrunnelseForInnsending != null) {
            søknad.medBegrunnelseForInnsending(begrunnelseForInnsending)
        }
    }

    private fun OpplaeringspengerSøknadDto.leggTilJournalposter(journalpostIder: Set<String>) {
        journalpostIder.forEach { journalpostId ->
            søknad.medJournalpost(
                Journalpost()
                    .medJournalpostId(journalpostId)
                    .medInformasjonSomIkkeKanPunsjes(harInfoSomIkkeKanPunsjes)
                    .medInneholderMedisinskeOpplysninger(harMedisinskeOpplysninger)
            )
        }
    }

    private fun OpplaeringspengerSøknadDto.leggTilLovbestemtFerie() {
        if (lovbestemtFerie.isNullOrEmpty()) {
            return
        }
        val k9LovbestemtFerie = mutableMapOf<Periode, LovbestemtFerie.LovbestemtFeriePeriodeInfo>()
        lovbestemtFerie?.filter { it.erSatt() }?.forEach { periode ->
            k9LovbestemtFerie[periode.somK9Periode()!!] =
                LovbestemtFerie.LovbestemtFeriePeriodeInfo().medSkalHaFerie(true)
        }
        opplaeringspenger.medLovbestemtFerie(LovbestemtFerie().medPerioder(k9LovbestemtFerie))
    }

    private fun List<OpplaeringspengerSøknadDto.UttakDto>?.leggTilUttak(søknadsperiode: List<PeriodeDto>?) {
        val k9Uttak = mutableMapOf<Periode, Uttak.UttakPeriodeInfo>()

        this?.filter { it.periode.erSatt() }?.forEach { uttak ->
            val k9Periode = uttak.periode!!.somK9Periode()!!
            val k9Info = Uttak.UttakPeriodeInfo()
            MappingUtils.mapEllerLeggTilFeil(
                feil = feil,
                felt = "ytelse.uttak.perioder.${k9Periode.jsonPath()}.timerPleieAvBarnetPerDag"
            ) { uttak.pleieAvBarnetPerDag?.somDuration() }?.also {
                k9Info.medTimerPleieAvBarnetPerDag(it)
            }
            k9Uttak[k9Periode] = k9Info
        }

        if (k9Uttak.isEmpty() && søknadsperiode != null) {
            søknadsperiode.forEach { periode ->
                periode.somK9Periode()?.let { k9Uttak[it] = MapOlpTilK9Format.DefaultUttak }
            }
        }

        if (k9Uttak.isNotEmpty()) {
            opplaeringspenger.medUttak(Uttak().medPerioder(k9Uttak))
        }
    }

    private fun erOpptjeningSatt(
        dto: OpplaeringspengerSøknadDto,
        opptjeningAktivitet: ArbeidAktivitetDto?
    ) = dto.opptjeningAktivitet != null &&
        (
            !opptjeningAktivitet?.arbeidstaker.isNullOrEmpty() ||
                opptjeningAktivitet?.frilanser != null ||
                opptjeningAktivitet?.selvstendigNaeringsdrivende != null
            )

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapOlpTilK9Format::class.java)

        private val Validator = OpplæringspengerSøknadValidator()
        private const val Versjon = "1.0.0"

        private val DefaultUttak =
            Uttak.UttakPeriodeInfo().medTimerPleieAvBarnetPerDag(Duration.ofHours(7).plusMinutes(30))
    }
}
