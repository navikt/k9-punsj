package no.nav.k9punsj.db.datamodell

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
    VET_IKKE("vetIkke");

    companion object {
        private val map = values().associateBy { v -> v.svar }
        fun fromKode(svar: String): TilsynsordningSvar {
            val type = map[svar]
            if (type != null) {
                return type
            } else {
                throw IllegalStateException("Fant ingen FagsakYtelseType med koden $svar")
            }
        }
    }

}
