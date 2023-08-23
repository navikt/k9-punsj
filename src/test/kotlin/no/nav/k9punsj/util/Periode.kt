package no.nav.k9punsj.util

import java.time.LocalDate
import java.util.Objects.requireNonNull

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate
) {
    init {
        requireNonNull(fom, "fom")
        requireNonNull(tom, "tom")
        require(!fom.isAfter(tom)) { "fom (fra-og-med dato) kan ikke være etter tom (til-og-med dato: $fom>$tom" }
    }
}
