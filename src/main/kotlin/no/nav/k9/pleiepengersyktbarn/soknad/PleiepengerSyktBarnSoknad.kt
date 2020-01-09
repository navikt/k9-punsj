package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

typealias Arbeidstid = String

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
        val signert: Boolean?,

        val beredskap: List<JaNeiMedTilleggsinformasjon>?,
        val nattevaak: List<JaNeiMedTilleggsinformasjon>?,

        @get:NotNull(message = MåSettes)
        val tilsynsordning: List<Tilsynsordning>?
)

enum class Språk {
        nb, nn
}

data class Periode(
        val fraOgMed: LocalDate?,
        val tilOgMed: LocalDate?
)

data class Barn(
        val norskIdent: String?,
        val foedselsdato: LocalDate?
)

data class Tilsynsordning(
        val periode: Periode,
        val mandag: Arbeidstid?,
        val tirsdag: Arbeidstid?,
        val onsdag: Arbeidstid?,
        val torsdag: Arbeidstid?,
        val fredag: Arbeidstid?
)

data class JaNeiMedTilleggsinformasjon(
        val svar: Boolean?,
        val tilleggsinformasjon: String?,
        val periode: Periode?
)
