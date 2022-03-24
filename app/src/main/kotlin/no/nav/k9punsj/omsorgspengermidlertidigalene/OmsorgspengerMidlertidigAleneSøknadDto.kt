package no.nav.k9punsj.omsorgspengermidlertidigalene

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.domenetjenester.dto.JournalpostIdDto
import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto
import no.nav.k9punsj.domenetjenester.dto.PeriodeDto
import no.nav.k9punsj.domenetjenester.dto.SøknadIdDto
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
