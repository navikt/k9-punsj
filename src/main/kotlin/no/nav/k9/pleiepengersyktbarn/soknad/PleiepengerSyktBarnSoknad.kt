package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class PleiepengerSyktBarnSoknad(
        @get:NotNull(message = "Fra og med må være satt")
        @get:Valid
        val fra_og_med: LocalDate?,
        @get:NotNull(message = "Til og med må være satt")
        @get:Valid
        val til_og_med: LocalDate?
)