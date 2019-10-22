package no.nav.k9.pleiepengersyktbarn.soknad

import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

data class Søknader(
        @get:Size(min = 1, message = "Må sendes minst en søknad")
        @get:Valid
        val innhold: List<Søknad>
)

data class Søknad(
        @get:NotNull(message = "Søker må være satt")
        @get:Valid
        val søker: Søker?
)

data class Søker(
        @get:NotNull(message = "Fødselsnummer må være satt")
        @get:NotBlank(message = "Fødselsnummer kan ikke være blankt")
        val fødselsnummer: String?
)