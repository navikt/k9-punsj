package no.nav.k9punsj.omsorgspengerutbetaling

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.fravær.AktivitetFravær
import no.nav.k9.søknad.felles.fravær.FraværPeriode
import no.nav.k9.søknad.felles.fravær.FraværÅrsak
import no.nav.k9.søknad.felles.fravær.SøknadÅrsak
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetalingSøknadValidator
import no.nav.k9punsj.felles.ZoneUtils.Oslo
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somDuration
import no.nav.k9punsj.felles.k9format.leggTilUtenlandsopphold
import no.nav.k9punsj.felles.k9format.mapOpptjeningAktivitet
import no.nav.k9punsj.felles.k9format.mapTilBosteder
import no.nav.k9punsj.utils.PeriodeUtils.somK9Periode
import no.nav.k9punsj.utils.StringUtils.erSatt
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZonedDateTime

internal class MapOmsUtTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    dto: OmsorgspengerutbetalingSøknadDto,
    eksisterendePerioder: List<PeriodeDto>
) {
    private val søknad = Søknad()
    private val omsorgspengerUtbetaling = OmsorgspengerUtbetaling()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            søknadId.leggTilSøknadId()
            Versjon.leggTilVersjon()
            dto.leggTilMottattDatoOgKlokkeslett()
            dto.soekerId?.leggTilSøker()
            dto.barn.leggTilBarn()
            dto.opptjeningAktivitet?.mapOpptjeningAktivitet(feil)?.apply {
                omsorgspengerUtbetaling.medAktivitet(this)
            }
            dto.leggTilJournalposter(journalpostIder = journalpostIder)
            dto.fravaersperioder?.leggTilFraværsperioder()

            dto.bosteder?.mapTilBosteder()?.apply {
                omsorgspengerUtbetaling.medBosteder(this)
            }

            dto.utenlandsopphold.leggTilUtenlandsopphold(feil).apply {
                omsorgspengerUtbetaling.medUtenlandsopphold(this)
            }

            // Fullfører søknad & validerer
            søknad.medYtelse(omsorgspengerUtbetaling)
            if (eksisterendePerioder.isNotEmpty()) {
                logger.info("Validerer søknad mot eksisterende perioder.")
                feil.addAll(
                    Validator.valider(
                        søknad,
                        eksisterendePerioder.map { it.somK9Periode() })
                )
            } else {
                logger.info("Validerer søknad.")
                feil.addAll(Validator.valider(søknad))
            }
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

    private fun OmsorgspengerutbetalingSøknadDto.leggTilMottattDatoOgKlokkeslett() {
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

    private fun String.leggTilSøker() {
        if (erSatt()) {
            søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
        }
    }

    private fun List<OmsorgspengerutbetalingSøknadDto.BarnDto>.leggTilBarn() {
        val barnListe = this.map {
            val barn = Barn()
            barn.medFødselsdato(it.foedselsdato)
            barn.medNorskIdentitetsnummer(NorskIdentitetsnummer.of(it.norskIdent))
        }
        omsorgspengerUtbetaling.medFosterbarn(barnListe)
    }

    private fun List<OmsorgspengerutbetalingSøknadDto.FraværPeriode>.leggTilFraværsperioder() {
        val fraværPerioder = mapIndexed { index, fraværsPeriode ->
            val periode: Periode? = fraværsPeriode.periode.somK9Periode()
            val timerBorte: Duration? = fraværsPeriode.tidPrDag?.somDuration()
            val normalArbeidstid: Duration? = fraværsPeriode.normalArbeidstid?.somDuration()
            val fraværÅrsak: FraværÅrsak? = fraværsPeriode.fraværÅrsak
            val søknadÅrsak: SøknadÅrsak? = fraværsPeriode.søknadÅrsak
            val aktivitetFravær: List<AktivitetFravær> = listOf(fraværsPeriode.aktivitetsFravær)
            if (AktivitetFravær.ARBEIDSTAKER == fraværsPeriode.aktivitetsFravær && fraværsPeriode.organisasjonsnummer.isNullOrEmpty()) {
                feil.add(
                    Feil(
                        "fraværsPerioder[$index].organisasjonsnummer",
                        null,
                        "organisasjonsnummer kan ikke være null dersom aktivitetsFravær er ARBEIDSTAKER"
                    )
                )
            }

            val organisasjonsnummer = Organisasjonsnummer.of(fraværsPeriode.organisasjonsnummer)

            val fraværsPeriode = FraværPeriode()
                .medPeriode(periode)
                .medFraværÅrsak(fraværÅrsak)
                .medSøknadsårsak(søknadÅrsak)
                .medAktivitetFravær(aktivitetFravær)
                .medArbeidsgiverOrgNr(organisasjonsnummer)

            if (timerBorte != null && timerBorte.isZero) {
                fraværsPeriode.medNulling()
            } else {
                fraværsPeriode
                    .medFravær(timerBorte)
                    .medNormalarbeidstid(normalArbeidstid)
            }
        }
        omsorgspengerUtbetaling.medFraværsperioder(fraværPerioder)
    }

    private fun OmsorgspengerutbetalingSøknadDto.leggTilJournalposter(journalpostIder: Set<String>) {
        journalpostIder.forEach { journalpostId ->
            søknad.medJournalpost(
                Journalpost()
                    .medJournalpostId(journalpostId)
                    .medInformasjonSomIkkeKanPunsjes(
                        harInfoSomIkkeKanPunsjes ?: false
                    ) // ikke nødvendig for korrigering av IM
                    .medInneholderMedisinskeOpplysninger(
                        harMedisinskeOpplysninger ?: false
                    ) // ikke nødvendig for korrigering av IM
            )
        }
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapOmsUtTilK9Format::class.java)
        private val Validator = OmsorgspengerUtbetalingSøknadValidator()
        private const val Versjon = "1.1.0" // støtte for normalArbeidstid.
    }
}
