package no.nav.k9punsj.søknad

import no.nav.k9.søknad.ytelse.psb.v1.UttakPeriodeInfo
import java.math.BigDecimal


data class Uttak(
    val perioder: Map<Periode, UttakPeriodeInfo>

)

data class UttakPeriodeInfo(
    val timerPleieAvBarnetPerDag: BigDecimal
)
