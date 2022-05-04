package no.nav.k9punsj.omsorgspengermidlertidigalene

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.AnnenForelder
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerKroniskSyktBarn
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerMidlertidigAlene
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.korrigeringinntektsmelding.MapOmsTilK9Format
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.ZonedDateTime

internal class MapOmsMATilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    dto: OmsorgspengerMidlertidigAleneSøknadDto,
) {
    private val søknad = Søknad()
    private val omsorgspengerMidlertidigAlene = OmsorgspengerMidlertidigAlene()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            søknadId.leggTilSøknadId()
            Versjon.leggTilVersjon()
            dto.leggTilMottattDatoOgKlokkeslett()
            dto.soekerId?.leggTilSøker()
            dto.barn.leggTilBarn()
            dto.annenForelder?.leggTilAnnenForelder()
            dto.leggTilJournalposter(journalpostIder = journalpostIder)

            // Fullfører søknad & validerer
            søknad.medYtelse(omsorgspengerMidlertidigAlene)
            feil.addAll(Validator.valider(søknad.getYtelse())) // TODO: 20/01/2022 Validerer ingenting...
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

    private fun OmsorgspengerMidlertidigAleneSøknadDto.leggTilMottattDatoOgKlokkeslett() {
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

    private fun OmsorgspengerMidlertidigAleneSøknadDto.AnnenForelder.leggTilAnnenForelder() {
        val annenForelder = AnnenForelder()
        annenForelder.medPeriode(this.periode?.somK9Periode())
        val situasjonType = this.situasjonType
        if (situasjonType != null) {
            annenForelder.medSituasjon(AnnenForelder.SituasjonType.valueOf(situasjonType), this.situasjonBeskrivelse)
        }
        annenForelder.medNorskIdentitetsnummer(NorskIdentitetsnummer.of(this.norskIdent))
        omsorgspengerMidlertidigAlene.medAnnenForelder(annenForelder)
    }

    private fun List<OmsorgspengerMidlertidigAleneSøknadDto.BarnDto>.leggTilBarn() {
        val barnListe = this.map {
            val barn = Barn()
            barn.medFødselsdato(it.foedselsdato)
            barn.medNorskIdentitetsnummer(NorskIdentitetsnummer.of(it.norskIdent))
        }.toTypedArray()
        omsorgspengerMidlertidigAlene.medBarn(*barnListe)
    }

    private fun String.leggTilSøker() {
        if (erSatt()) {
            søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
        }
    }

    private fun OmsorgspengerMidlertidigAleneSøknadDto.leggTilJournalposter(journalpostIder: Set<String>) {
        journalpostIder.forEach { journalpostId ->
            søknad.medJournalpost(
                Journalpost()
                    .medJournalpostId(journalpostId)
                    .medInformasjonSomIkkeKanPunsjes(harInfoSomIkkeKanPunsjes)
                    .medInneholderMedisinskeOpplysninger(harMedisinskeOpplysninger)

            )
        }
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapOmsMATilK9Format::class.java)
        private val Oslo = ZoneId.of("Europe/Oslo")
        private val Validator = OmsorgspengerKroniskSyktBarn().validator
        private const val Versjon = "1.0.0"
        private fun PeriodeDto?.erSatt() = this != null && (fom != null || tom != null)
        private fun PeriodeDto.somK9Periode() = when (erSatt()) {
            true -> Periode(fom, tom)
            else -> null
        }

        private fun String?.erSatt() = !isNullOrBlank()
    }
}
