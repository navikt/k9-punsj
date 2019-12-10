package no.nav.k9.pleiepengersyktbarn.soknad

import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidPleiepengerSyktBarnSoknad
data class PleiepengerSyktBarnSoknad(
        @get:NotNull(message = "PERIODER_MAA_SETTES")
        @get:Size(min=1, message = "MINST_EN_PERIODE_MAA_SETTES")
        @get:Valid
        val perioder: Set<Periode>?,

        @get:NotNull(message = "SPRAAK_MAA_SETTES")
        val spraak: Språk?,

        @get:NotNull(message = "BARN_MAA_SETTES")
        @get:Valid
        val barn: Barn?,

        @get:NotNull(message = "SOEKNADEN_MAA_SIGNERES")
        val signert: Boolean?
)

enum class Språk {
        nb, nn
}

data class Periode(
        @get:NotNull(message = "FRA_OG_MED_MAA_VAERE_SATT")
        @get:Valid
        val fra_og_med: LocalDate?,
        @get:NotNull(message = "TIL_OG_MED_MAA_VAERE_SATT")
        @get:Valid
        val til_og_med: LocalDate?,

        @get:Valid
        val beredskap: JaNeiMedTilleggsinformasjon?,
        @get:Valid
        val nattevaak: JaNeiMedTilleggsinformasjon?
)

data class Barn(
        val norsk_ident: String?,
        val foedselsdato: LocalDate?
)

data class JaNeiMedTilleggsinformasjon(
        @get:NotNull(message = "SVAR_MAA_SETTES")
        val svar: Boolean?,
        val tilleggsinformasjon: String?
)