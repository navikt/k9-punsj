package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.domenetjenester.dto.JournalpostIdDto
import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto
import no.nav.k9punsj.domenetjenester.dto.SøknadIdDto
import java.time.LocalDate
import java.time.LocalTime


data class OmsorgspengerKroniskSyktBarnSøknadDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett : LocalTime? = null,
    val barn: BarnDto? = null,
    val journalposter: List<JournalpostIdDto>? = null,
    val harInfoSomIkkeKanPunsjes : Boolean,
    val harMedisinskeOpplysninger : Boolean
) {
    data class BarnDto(
        val norskIdent: NorskIdentDto?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )
}
