package no.nav.k9punsj.omsorgspengermidlertidigalene

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.Mappe
import no.nav.k9punsj.domenetjenester.dto.*
import no.nav.k9punsj.domenetjenester.dto.hentUtJournalposter
import no.nav.k9punsj.objectMapper
import java.time.LocalDate
import java.time.LocalTime


data class OmsorgspengerMidlertidigAleneSøknadDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett : LocalTime? = null,
    val barn: List<BarnDto> = emptyList(),
    val annenForelder: AnnenForelder? = null,
    val journalposter: List<JournalpostIdDto>? = null,
    val harInfoSomIkkeKanPunsjes : Boolean? = null,
    val harMedisinskeOpplysninger : Boolean? = null
) {
    data class BarnDto(
        val norskIdent: NorskIdentDto?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )

    data class AnnenForelder(
        val norskIdent: NorskIdentDto?,
        val situasjonType: String?,
        val situasjonBeskrivelse: String?,
        val periode: PeriodeDto?,
    )
}

data class SvarOmsMADto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerMidlertidigAleneSøknadDto>?,
)

internal fun Mappe.tilOmsMAVisning(norskIdent: NorskIdentDto): SvarOmsMADto {
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
