package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.fravær.AktivitetFravær
import no.nav.k9.søknad.felles.fravær.FraværPeriode
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetalingSøknadValidator
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.domenetjenester.dto.OmsorgspengerSøknadDto
import no.nav.k9punsj.domenetjenester.dto.PeriodeDto
import no.nav.k9punsj.domenetjenester.dto.PleiepengerSyktBarnSøknadDto
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

internal class MapOmsTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    dto: OmsorgspengerSøknadDto,
) {
    private val søknad = Søknad()
    private val omsorgspengerUtbetaling = OmsorgspengerUtbetaling()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            søknadId.leggTilSøknadId()
            Versjon.leggTilVersjon()
            dto.leggTilMottattDato()
            dto.soekerId?.leggTilSøker()
            dto.leggTilJournalposter(journalpostIder = journalpostIder)
            dto.fravaersperioder?.leggTilFraværsperioderKorrigeringIm(dto)

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

    private fun OmsorgspengerSøknadDto.leggTilMottattDato() { if (mottattDato != null && klokkeslett != null) {
        søknad.medMottattDato(ZonedDateTime.of(mottattDato, klokkeslett, Oslo))
    }}

    private fun String.leggTilSøker() {
        if (erSatt()) {
            søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
        }
    }

    private fun List<OmsorgspengerSøknadDto.FraværPeriode>.leggTilFraværsperioderKorrigeringIm(dto: OmsorgspengerSøknadDto) {

        val perioderMedTrekkAvDager = filter { it.periode.erSatt() }
            .filter { it.tidPrDag?.somDuration() == Duration.ZERO }.map {
                val somEnkeltDager = it.periode.somEnkeltDager()
                somEnkeltDager.map { dag ->
                    FraværPeriode(
                        dag.somK9Periode(),
                        Duration.ZERO,
                        null,
                        null,
                        listOf(AktivitetFravær.ARBEIDSTAKER),
                        Organisasjonsnummer.of(dto.organisasjonsnummer),
                        dto.arbeidsforholdId
                    )
                }
            }.flatten()

        val fullDagListe = filter { it.periode.erSatt() }
            .filter { it.faktiskTidPrDag == null }
            .map {
                FraværPeriode(
                    it.periode.somK9Periode(),
                    it.tidPrDag?.somDuration(),
                    null,
                    null,
                    listOf(AktivitetFravær.ARBEIDSTAKER),
                    Organisasjonsnummer.of(dto.organisasjonsnummer),
                    dto.arbeidsforholdId
                )
            }.toList()

        val delvisDagListe = filter { it.periode.erSatt() }
            .filter { it.faktiskTidPrDag != null && it.tidPrDag?.somDuration() != Duration.ZERO }
            .map {
                FraværPeriode(
                    it.periode.somK9Periode(),
                    it.tidPrDag?.somDuration(),
                    null,
                    null,
                    listOf(AktivitetFravær.ARBEIDSTAKER),
                    Organisasjonsnummer.of(dto.organisasjonsnummer),
                    dto.arbeidsforholdId
                )
            }.toList()

        val fraværsperioder : MutableList<FraværPeriode> = mutableListOf()
        fraværsperioder.addAll(perioderMedTrekkAvDager)
        fraværsperioder.addAll(fullDagListe)
        fraværsperioder.addAll(delvisDagListe)

        omsorgspengerUtbetaling.medFraværsperioderKorrigeringIm(fraværsperioder)
    }

    private fun OmsorgspengerSøknadDto.leggTilJournalposter(journalpostIder: Set<JournalpostId>) {
        journalpostIder.forEach { journalpostId ->
            søknad.medJournalpost(Journalpost()
                .medJournalpostId(journalpostId)
                .medInformasjonSomIkkeKanPunsjes(harInfoSomIkkeKanPunsjes ?: false) // ikke nødvendig for korrigering av IM
                .medInneholderMedisinskeOpplysninger(harMedisinskeOpplysninger ?: false) // ikke nødvendig for korrigering av IM
            )
        }
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapOmsTilK9Format::class.java)
        private val Oslo = ZoneId.of("Europe/Oslo")
        private val Validator = OmsorgspengerUtbetalingSøknadValidator()
        private const val Versjon = "1.0.0"
        private fun PeriodeDto?.erSatt() = this != null && (fom != null || tom != null)
        private fun PeriodeDto.somK9Periode() = when (erSatt()) {
            true -> Periode(fom, tom)
            else -> null
        }

        private fun PeriodeDto.somEnkeltDager() : List<PeriodeDto> {
            val lista: MutableList<PeriodeDto> = mutableListOf()
            for (i in 0 until Duration.between(fom?.atStartOfDay(), tom?.plusDays(1)?.atStartOfDay()).toDays()) {
                lista.add(PeriodeDto(fom?.plusDays(i), fom?.plusDays(i)))
            }
            return lista
        }

        private fun String?.erSatt() = !isNullOrBlank()
        private fun PleiepengerSyktBarnSøknadDto.TimerOgMinutter.somDuration() =
            Duration.ofHours(timer).plusMinutes(minutter.toLong())
    }
}
