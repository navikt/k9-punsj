package no.nav.k9punsj.omsorgspengermidlertidigalene

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.Mappe
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SøknadDto
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.objectMapper
import java.time.LocalDate
import java.time.LocalTime

data class OmsorgspengerMidlertidigAleneSøknadDto(
    override val soeknadId: String,
    override val soekerId: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    override val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    override val klokkeslett : LocalTime? = null,
    val barn: List<BarnDto> = emptyList(),
    val annenForelder: AnnenForelder? = null,
    override val journalposter: List<String>? = null,
    val harInfoSomIkkeKanPunsjes : Boolean? = null,
    val harMedisinskeOpplysninger : Boolean? = null
): SøknadDto {
    data class BarnDto(
        val norskIdent: String?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )

    data class AnnenForelder(
        val norskIdent: String?,
        val situasjonType: String?,
        val situasjonBeskrivelse: String?,
        val periode: PeriodeDto?,
    )
}

data class SvarOmsMADto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerMidlertidigAleneSøknadDto>?,
)

internal fun Mappe.tilOmsMAVisning(norskIdent: String): SvarOmsMADto {
    val bunke = hentFor(FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsMADto(norskIdent, FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                OmsorgspengerMidlertidigAleneSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s))
            }
        }
    return SvarOmsMADto(norskIdent, FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE.kode, søknader)
}

internal fun SøknadEntitet.tilOmsMAvisning(): OmsorgspengerMidlertidigAleneSøknadDto {
    if (søknad == null) {
        return OmsorgspengerMidlertidigAleneSøknadDto(soeknadId = this.søknadId)
    }
    return objectMapper().convertValue(søknad)
}
