package no.nav.k9punsj.journalpost.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class VentDto(
    val venteÅrsak: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val venterTil: LocalDate,
)
