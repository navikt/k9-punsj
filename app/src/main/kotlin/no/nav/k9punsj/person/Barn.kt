package no.nav.k9punsj.person

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class Barn(
    val identitetsnummer: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fødselsdato: LocalDate,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String) {
    val sammensattNavn = when (mellomnavn) {
        null -> "$fornavn $etternavn"
        else -> "$fornavn $mellomnavn $etternavn"
    }
}

data class BarnResponse(
    val barn: Set<Barn>
)