package no.nav.k9punsj.brev.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.LocalTime

data class BrevVisningDto(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime,
    val journalpostId: String?,
    val mottaker: MottakerDto,
    val saksnummer: String,
    val sendtInnAv: String,
)


