package no.nav.k9punsj.utils

import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9punsj.felles.dto.PeriodeDto

object PeriodeUtils {
    fun PeriodeDto?.erSatt() = this != null && (fom != null || tom != null)
    fun PeriodeDto.somK9Periode() = when (erSatt()) {
        true -> Periode(fom, tom)
        else -> null
    }

    fun Collection<PeriodeDto>.somK9Perioder() = mapNotNull { it.somK9Periode() }

    fun Periode.jsonPath() = "[${this.iso8601}]"

    fun LocalDateInterval.somK9Periode(): Periode {
        return Periode(this.fomDato, this.tomDato)
    }
}
