package no.nav.k9punsj.omsorgspengeraleneomsorg

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.Mappe
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.utils.objectMapper
import java.time.LocalDate
import java.time.LocalTime

data class OmsorgspengerAleneOmsorgSøknadDto(
    val soeknadId: String,
    val soekerId: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime? = null,
    val barn: BarnDto? = null,
    val journalposter: List<String>? = null,
    val periode: PeriodeDto? = null,
    val begrunnelseForInnsending: String? = null,
    val harInfoSomIkkeKanPunsjes: Boolean? = null,
    val harMedisinskeOpplysninger: Boolean? = null,
    val metadata: Map<*, *>? = null,
    val k9saksnummer: String? = null
) {
    data class BarnDto(
        val norskIdent: String?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )
}

data class SvarOmsAODto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerAleneOmsorgSøknadDto>?
)

internal fun Mappe.tilOmsAOVisning(norskIdent: String): SvarOmsAODto {
    val bunke = hentFor(FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsAODto(norskIdent, FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue<OmsorgspengerAleneOmsorgSøknadDto>(s.søknad).copy(
                    k9saksnummer = s.k9saksnummer
                )
            } else {
                OmsorgspengerAleneOmsorgSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s), k9saksnummer = s.k9saksnummer)
            }
        }
    return SvarOmsAODto(norskIdent, FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN.kode, søknader)
}

internal fun SøknadEntitet.tilOmsAOvisning(): OmsorgspengerAleneOmsorgSøknadDto {
    if (søknad == null) {
        return OmsorgspengerAleneOmsorgSøknadDto(soeknadId = this.søknadId, k9saksnummer = k9saksnummer)
    }
    return objectMapper().convertValue<OmsorgspengerAleneOmsorgSøknadDto>(søknad).copy(
        k9saksnummer = k9saksnummer
    )
}
