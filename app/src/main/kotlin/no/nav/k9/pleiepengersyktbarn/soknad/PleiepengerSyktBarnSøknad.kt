package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.Duration
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidPleiepengerSyktBarnSoknad
data class PleiepengerSyktBarnSoknad(

        @get:NotNull(message = MåSettes)
        val datoMottatt: LocalDate?,

        @get:NotNull(message = MåSettes)
        @get:Size(min=1, message = MinstEnMåSettes)
        @get:Valid
        val perioder: List<Periode>?,

        @get:NotNull(message = MåSettes)
        val spraak: Språk?,

        @get:NotNull(message = MåSettes)
        @get:Valid
        val barn: Barn?,

        val beredskap: List<PeriodeMedTilleggsinformasjon>?,
        val nattevaak: List<PeriodeMedTilleggsinformasjon>?,

        @get:NotNull(message = MåSettes)
        val tilsynsordning: Tilsynsordning?,

        @get:NotNull(message = MåSettes)
        val arbeid: Arbeid?
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
        val iTilsynsordning: JaNeiVetikke?,
        val opphold: List<Opphold>
)

data class Arbeid (
        val arbeidstaker: List<Arbeidsgiver>?,
        val selvstendigNaeringsdrivende: List<Oppdragsforhold>?,
        val frilanser: List<Oppdragsforhold>?
)

data class Arbeidsgiver(
        val skalJobbeProsent: List<Tilstedevaerelsesgrad>?,
        val organisasjonsnummer: String?,
        val norskIdent: String?
)

data class Tilstedevaerelsesgrad(
        val periode: Periode?,
        val grad: Float?
)

data class Oppdragsforhold(
        val periode: Periode?
)

data class Opphold(
        val periode: Periode?,
        val mandag: Duration?,
        val tirsdag: Duration?,
        val onsdag: Duration?,
        val torsdag: Duration?,
        val fredag: Duration?
)

enum class JaNeiVetikke { ja, nei, vetIkke }

data class PeriodeMedTilleggsinformasjon(
        val tilleggsinformasjon: String?,
        val periode: Periode?
)