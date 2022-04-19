package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerKroniskSyktBarn
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.ZonedDateTime

internal class MapOmsKSBTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    dto: OmsorgspengerKroniskSyktBarnSøknadDto,
) {
    private val søknad = Søknad()
    private val omsorgspengerKroniskSyktBarn = OmsorgspengerKroniskSyktBarn()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            søknadId.leggTilSøknadId()
            Versjon.leggTilVersjon()
            dto.leggTilMottattDato()
            dto.soekerId?.leggTilSøker()
            dto.barn?.leggTilBarn()
            leggTilKroniskEllerFunksjonshemming()
            dto.leggTilJournalposter(journalpostIder = journalpostIder)

            // Fullfører søknad & validerer
            søknad.medYtelse(omsorgspengerKroniskSyktBarn)
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

    private fun OmsorgspengerKroniskSyktBarnSøknadDto.leggTilMottattDato() {
        if (mottattDato != null && klokkeslett != null) {
            søknad.medMottattDato(ZonedDateTime.of(mottattDato, klokkeslett, Oslo))
        }
    }

    private fun leggTilKroniskEllerFunksjonshemming() {
        omsorgspengerKroniskSyktBarn.medKroniskEllerFunksjonshemming(true)
    }

    private fun OmsorgspengerKroniskSyktBarnSøknadDto.BarnDto.leggTilBarn() = when {
        norskIdent != null -> omsorgspengerKroniskSyktBarn.medBarn(
            Barn().medNorskIdentitetsnummer(
                NorskIdentitetsnummer.of(
                    norskIdent
                )
            )
        )
        foedselsdato != null -> omsorgspengerKroniskSyktBarn.medBarn(Barn().medFødselsdato(foedselsdato))
        else -> omsorgspengerKroniskSyktBarn.medBarn(Barn())
    }

    private fun String.leggTilSøker() {
        if (erSatt()) {
            søknad.medSøker(Søker(NorskIdentitetsnummer.of(this)))
        }
    }

    private fun OmsorgspengerKroniskSyktBarnSøknadDto.leggTilJournalposter(journalpostIder: Set<String>) {
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
        private val logger = LoggerFactory.getLogger(MapOmsKSBTilK9Format::class.java)
        private val Oslo = ZoneId.of("Europe/Oslo")
        private val Validator = OmsorgspengerKroniskSyktBarn().validator
        private const val Versjon = "1.0.0"

        private fun String?.erSatt() = !isNullOrBlank()
    }
}
