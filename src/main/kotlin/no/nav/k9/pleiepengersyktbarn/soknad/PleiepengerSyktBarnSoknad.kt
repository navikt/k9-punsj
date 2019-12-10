package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

data class PleiepengerSyktBarnSoknad(
        @get:NotNull(message = "Perioder må settes")
        @get:Size(min=1, message = "Må sette minst en periode")
        val perioder: Set<Perioder>?,

        @get:NotNull(message = "Språk må settes")
        val spraak: Språk?,

        @get:NotNull(message = "Barn må settes")
        @get:Valid
        val barn: Barn?,

        @get:NotNull(message = "Søknaden må være signert")
        val signert: Boolean?
)

enum class Språk {
        nb, nn
}

data class Perioder(
        @get:NotNull(message = "Fra og med må være satt")
        @get:Valid
        val fra_og_med: LocalDate?,
        @get:NotNull(message = "Til og med må være satt")
        @get:Valid
        val til_og_med: LocalDate?,

        val beredskap: JaNeiMedTilleggsinformasjon?,
        val nattevaak: JaNeiMedTilleggsinformasjon?
)

data class Barn(
        val norsk_ident: String?,
        val foedselsdato: LocalDate?
)

data class JaNeiMedTilleggsinformasjon(
        @get:NotNull(message = "Svar må settes true / false")
        val svar: Boolean?,
        val tilleggsinformasjon: String?
)