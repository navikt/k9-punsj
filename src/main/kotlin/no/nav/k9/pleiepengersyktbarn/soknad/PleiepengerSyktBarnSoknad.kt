package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidPleiepengerSyktBarnSoknad
data class PleiepengerSyktBarnSoknad(
        @get:NotNull(message = MåSettes)
        @get:Size(min=1, message = MinstEnMåSettes)
        @get:Valid
        val perioder: List<Periode>?,

        @get:NotNull(message = MåSettes)
        val spraak: Språk?,

        @get:NotNull(message = MåSettes)
        @get:Valid
        val barn: Barn?,

        @get:NotNull(message = MåSettes)
        val signert: Boolean?
)

enum class Språk {
        nb, nn
}

data class Periode(
        val fra_og_med: LocalDate?,
        val til_og_med: LocalDate?,
        val beredskap: JaNeiMedTilleggsinformasjon?,
        val nattevaak: JaNeiMedTilleggsinformasjon?
)

data class Barn(
        val norsk_ident: String?,
        val foedselsdato: LocalDate?
)

data class JaNeiMedTilleggsinformasjon(
        val svar: Boolean?,
        val tilleggsinformasjon: String?
)