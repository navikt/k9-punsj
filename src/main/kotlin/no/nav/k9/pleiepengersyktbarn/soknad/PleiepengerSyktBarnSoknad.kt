package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class PersonligDel(
        @get:NotNull(message = "Søker må være satt")
        @get:Valid
        val søker: Søker?
)

data class FellesDel(
        @get:NotNull(message = "Fra og med må være satt")
        @get:Valid
        val fraOgMed: LocalDate,
        @get:NotNull(message = "Til og med må være satt")
        @get:Valid
        val tilOgMed: LocalDate
)

data class Søker(
        @get:NotNull(message = "Fødselsnummer må være satt")
        @get:NotBlank(message = "Fødselsnummer kan ikke være blankt")
        val fødselsnummer: String?
)