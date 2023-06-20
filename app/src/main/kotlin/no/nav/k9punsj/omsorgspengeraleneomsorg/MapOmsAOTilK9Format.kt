package no.nav.k9punsj.omsorgspengeraleneomsorg

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerAleneOmsorg
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerAleneOmsorgSøknadValidator
import no.nav.k9punsj.felles.ZoneUtils.Oslo
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.utils.PeriodeUtils.somK9Periode
import no.nav.k9punsj.utils.StringUtils.erSatt
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZonedDateTime

internal class MapOmsAOTilK9Format(
    søknadId: String,
    journalpostIder: Set<String>,
    dto: OmsorgspengerAleneOmsorgSøknadDto
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
            dto.leggtilBegrunnelseForInnsending()
            val omsorgspengerAleneOmsorg = OmsorgspengerAleneOmsorg(
                dto.barn?.leggTilBarn(),
                dto.periode?.utledDato()?.somK9Periode()
            )

            // Fullfører søknad & validerer
            søknad.medYtelse(omsorgspengerAleneOmsorg)
            feil.addAll(Validator.valider(søknad))
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

    private fun OmsorgspengerAleneOmsorgSøknadDto.leggtilBegrunnelseForInnsending() {
        if(!this.begrunnelseForInnsending.isNullOrEmpty()) {
            val begrunnelseForInnsending = BegrunnelseForInnsending()
                .medBegrunnelseForInnsending(this.begrunnelseForInnsending)
            søknad.medBegrunnelseForInnsending(begrunnelseForInnsending)
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

        private val Validator = OmsorgspengerAleneOmsorgSøknadValidator()
        private const val Versjon = "1.0.0"
    }
}

fun PeriodeDto.utledDato(): PeriodeDto {
    if (fom == null) return this

    return when {
        fom.siste2Årene() -> PeriodeDto(fom, tom)
        else -> PeriodeDto(LocalDate.now().minusYears(1).startenAvÅret(), tom)
    }
}

private fun LocalDate.siste2Årene(): Boolean {
    val dagensDato = LocalDate.now()
    val startenAvIfjor = LocalDate.of(dagensDato.year - 1, 1, 1)
    val sluttenAvDetteÅret = LocalDate.of(dagensDato.year, 12, 31)
    return this in startenAvIfjor..sluttenAvDetteÅret
}

private fun LocalDate.startenAvÅret() = LocalDate.parse("${year}-01-01")
