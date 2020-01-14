package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.Duration
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
        val signert: Boolean?,

        val beredskap: List<JaNeiMedTilleggsinformasjon>?,
        val nattevaak: List<JaNeiMedTilleggsinformasjon>?,

        @get:NotNull(message = MåSettes)
        val tilsynsordning: Tilsynsordning?,

        @get:NotNull(message = MåSettes)
        val arbeidsgivere: Virksomheter?
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
        val itilsynsordning: JaNeiVetikke?,
        val opphold: List<Opphold>
)

data class Virksomheter (
        val arbeidsforhold: Arbeidsforhold?,
        val selvstendigNaeringsdrivende: Oppdragsforhold?,
        val frilans: Oppdragsforhold?
)

data class Arbeidsforhold(
        val periode: Periode,
        val skalJobbeProsent: Float,
        val organisasjonsnummer: String,
        val norskIdent: String?
)

data class Oppdragsforhold(
        val periode: Periode
)

data class Opphold(
        val periode: Periode,
        val mandag: Duration?,
        val tirsdag: Duration?,
        val onsdag: Duration?,
        val torsdag: Duration?,
        val fredag: Duration?
)

enum class JaNeiVetikke { ja, nei, vetIkke }

data class JaNeiMedTilleggsinformasjon(
        val svar: Boolean?,
        val tilleggsinformasjon: String?,
        val periode: Periode?
)