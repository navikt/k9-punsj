package no.nav.k9punsj.omsorgspengeraleneomsorg

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerAleneOmsorg
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerKroniskSyktBarn
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.korrigeringinntektsmelding.MapOmsTilK9Format
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.ZonedDateTime

internal class MapOmsAOTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    dto: OmsorgspengerAleneOmsorgSøknadDto,
) {
    private val søknad = Søknad()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            søknadId.leggTilSøknadId()
            Versjon.leggTilVersjon()
            dto.leggTilMottattDatoOgKlokkeslett()
            dto.soekerId?.leggTilSøker()
            dto.leggTilJournalposter(journalpostIder = journalpostIder)
            val omsorgspengerAleneOmsorg = OmsorgspengerAleneOmsorg(
                dto.barn?.leggTilBarn(),
                dto.soeknadsperiode?.somK9Periode(),
                dto.begrunnelseForInnsending
            )

            // Fullfører søknad & validerer
            søknad.medYtelse(omsorgspengerAleneOmsorg)
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

    private fun OmsorgspengerAleneOmsorgSøknadDto.leggTilMottattDatoOgKlokkeslett() {
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


    private fun OmsorgspengerAleneOmsorgSøknadDto.BarnDto.leggTilBarn(): Barn = when {
        norskIdent != null ->
            Barn().medNorskIdentitetsnummer(
                NorskIdentitetsnummer.of(
                    norskIdent
                )
            )
        foedselsdato != null -> Barn().medFødselsdato(foedselsdato)
        else -> Barn()
    }

    private fun String.leggTilSøker() {
        if (erSatt()) {
            søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
        }
    }

    private fun OmsorgspengerAleneOmsorgSøknadDto.leggTilJournalposter(journalpostIder: Set<String>) {
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
        private val logger = LoggerFactory.getLogger(MapOmsAOTilK9Format::class.java)
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
