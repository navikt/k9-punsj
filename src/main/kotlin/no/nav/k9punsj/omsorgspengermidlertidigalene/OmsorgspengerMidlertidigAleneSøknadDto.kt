package no.nav.k9punsj.omsorgspengermidlertidigalene

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.dto.Mappe
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.utils.objectMapper
import java.time.LocalDate
import java.time.LocalTime

data class NyOmsMASøknad(
    val norskIdent: String,
    val journalpostId: String,
    val annenPart: String? = null,
    val barn: List<OmsorgspengerMidlertidigAleneSøknadDto.BarnDto>,
    val k9saksnummer: String?
)

data class OmsorgspengerMidlertidigAleneSøknadDto(
    val soeknadId: String,
    val soekerId: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime? = null,
    val barn: List<BarnDto> = emptyList(),
    val annenForelder: AnnenForelder? = null,
    val journalposter: List<String>? = null,
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

    data class AnnenForelder(
        val norskIdent: String?,
        val situasjonType: String?,
        val situasjonBeskrivelse: String?,
        val periode: PeriodeDto?
    )
}

data class SvarOmsMADto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerMidlertidigAleneSøknadDto>?
)

internal fun Mappe.tilOmsMAVisning(norskIdent: String): SvarOmsMADto {
    val bunke = hentFor(PunsjFagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsMADto(norskIdent, PunsjFagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue<OmsorgspengerMidlertidigAleneSøknadDto>(s.søknad).copy(
                    k9saksnummer = s.k9saksnummer
                )
            } else {
                OmsorgspengerMidlertidigAleneSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s), k9saksnummer = s.k9saksnummer)
            }
        }
    return SvarOmsMADto(norskIdent, PunsjFagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE.kode, søknader)
}

internal fun SøknadEntitet.tilOmsMAvisning(): OmsorgspengerMidlertidigAleneSøknadDto {
    if (søknad == null) {
        return OmsorgspengerMidlertidigAleneSøknadDto(soeknadId = this.søknadId, k9saksnummer = k9saksnummer)
    }
    return objectMapper().convertValue<OmsorgspengerMidlertidigAleneSøknadDto>(søknad).copy(
        k9saksnummer = k9saksnummer
    )
}
