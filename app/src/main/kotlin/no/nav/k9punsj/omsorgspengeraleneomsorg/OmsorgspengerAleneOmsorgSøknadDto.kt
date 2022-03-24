package no.nav.k9punsj.omsorgspengeraleneomsorg

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.Mappe
import no.nav.k9punsj.db.datamodell.SøknadEntitet
import no.nav.k9punsj.domenetjenester.dto.*
import no.nav.k9punsj.domenetjenester.dto.hentUtJournalposter
import no.nav.k9punsj.objectMapper
import java.time.LocalDate
import java.time.LocalTime


data class OmsorgspengerAleneOmsorgSøknadDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett : LocalTime? = null,
    val barn: BarnDto? = null,
    val journalposter: List<JournalpostIdDto>? = null,
    val soeknadsperiode: PeriodeDto? = null,
    val begrunnelseForInnsending: String? = null,
    val harInfoSomIkkeKanPunsjes : Boolean? = null,
    val harMedisinskeOpplysninger : Boolean? = null,
) {
    data class BarnDto(
        val norskIdent: NorskIdentDto?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )
}

data class SvarOmsAODto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerAleneOmsorgSøknadDto>?,
)

internal fun Mappe.tilOmsAOVisning(norskIdent: NorskIdentDto): SvarOmsAODto {
    val bunke = hentFor(FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsAODto(norskIdent, FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                OmsorgspengerAleneOmsorgSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s))
            }
        }
    return SvarOmsAODto(norskIdent, FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN.kode, søknader)
}

internal fun SøknadEntitet.tilOmsAOvisning(): OmsorgspengerAleneOmsorgSøknadDto {
    if (søknad == null) {
        return OmsorgspengerAleneOmsorgSøknadDto(soeknadId = this.søknadId)
    }
    return objectMapper().convertValue(søknad)
}
