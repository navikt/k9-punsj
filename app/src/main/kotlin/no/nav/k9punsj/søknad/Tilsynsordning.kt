package no.nav.k9punsj.søknad

import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynsordningOpphold
import java.time.Duration


data class Tilsynsordning(
    val tilsynsordningSvar: TilsynsordningSvar,
    val opphold: Map<Periode, TilsynsordningOpphold>
)

data class TilsynsordningOpphold(
    val lengde: Duration

)

enum class TilsynsordningSvar(val svar: String) {
    JA("ja"),
    NEI("nei"),
    VET_IKKE("vetIkke")

}
