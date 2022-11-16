package no.nav.k9punsj.opplaeringspenger

import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.type.Journalpost
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.ytelse.olp.v1.Opplæringspenger
import no.nav.k9.søknad.ytelse.olp.v1.OpplæringspengerSøknadValidator
import no.nav.k9punsj.felles.ZoneUtils.Oslo
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.k9format.leggTilUtenlandsopphold
import no.nav.k9punsj.felles.k9format.mapTilArbeidstid
import no.nav.k9punsj.utils.PeriodeUtils.somK9Perioder
import no.nav.k9punsj.utils.StringUtils.erSatt
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

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
            dto.leggTilBegrunnelseForInnsending()

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

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapOlpTilK9Format::class.java)

        private val Validator = OpplæringspengerSøknadValidator()
        private const val Versjon = "1.0.0"
    }
}
