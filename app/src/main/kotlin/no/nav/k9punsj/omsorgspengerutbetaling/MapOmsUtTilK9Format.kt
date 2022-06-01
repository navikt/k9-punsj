package no.nav.k9punsj.omsorgspengerutbetaling

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.fravær.AktivitetFravær
import no.nav.k9.søknad.felles.fravær.FraværPeriode
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetalingSøknadValidator
import no.nav.k9punsj.felles.ZoneUtils.Oslo
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somDuration
import no.nav.k9punsj.felles.k9format.mapOpptjeningAktivitet
import no.nav.k9punsj.utils.PeriodeUtils.somK9Periode
import no.nav.k9punsj.utils.StringUtils.erSatt
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZonedDateTime

internal class MapOmsUtTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    dto: OmsorgspengerutbetalingSøknadDto,
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

            // Fullfører søknad & validerer
            søknad.medYtelse(omsorgspengerUtbetaling)
            feil.addAll(Validator.valider(søknad))
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
            val periode = fraværsPeriode.periode.somK9Periode()
            val timerBorte = fraværsPeriode.tidPrDag?.somDuration()
            val fraværÅrsak = fraværsPeriode.fraværÅrsak
            val søknadÅrsak = fraværsPeriode.søknadÅrsak
            val aktivitetFravær = listOf(fraværsPeriode.aktivitetsFravær)
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

            FraværPeriode(periode, timerBorte, fraværÅrsak, søknadÅrsak, aktivitetFravær, organisasjonsnummer, null)
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
        private const val Versjon = "1.0.0"

        private fun PeriodeDto.somEnkeltDager(): List<PeriodeDto> {
            val lista: MutableList<PeriodeDto> = mutableListOf()
            for (i in 0 until Duration.between(fom?.atStartOfDay(), tom?.plusDays(1)?.atStartOfDay()).toDays()) {
                lista.add(PeriodeDto(fom?.plusDays(i), fom?.plusDays(i)))
            }
            return lista
        }
    }
}
