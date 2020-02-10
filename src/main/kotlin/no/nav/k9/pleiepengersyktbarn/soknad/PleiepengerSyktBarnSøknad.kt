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

        val beredskap: List<JaNeiMedTilleggsinformasjon>?,
        val nattevaak: List<JaNeiMedTilleggsinformasjon>?,

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
        val arbeidstaker: List<Arbeidsperiode>?,
        val selvstendigNaeringsdrivende: List<Oppdragsforhold>?,
        val frilanser: List<Oppdragsforhold>?
)

data class Arbeidsperiode(
        val periode: Periode?,
        val skalJobbeProsent: Float?,
        val organisasjonsnummer: String?,
        val norskIdent: String?
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

data class JaNeiMedTilleggsinformasjon(
        val svar: Boolean?,
        val tilleggsinformasjon: String?,
        val periode: Periode?
)