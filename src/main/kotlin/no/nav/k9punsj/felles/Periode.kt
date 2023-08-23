package no.nav.k9punsj.felles

import java.time.LocalDate

data class Periode(internal val fom: LocalDate?, internal val tom: LocalDate?) {
    override fun toString() = "${fom.iso8601()}/${tom.iso8601()}"
    internal fun erÅpenPeriode() = this == ÅpenPeriode

    internal companion object {
        private const val Åpen = ".."

        private fun LocalDate?.iso8601() = when (this) {
            null -> Åpen
            else -> "$this"
        }

        internal val ÅpenPeriode = Periode(null,null)
        internal fun String.somPeriode() : Periode {
            val split = split("/")
            require(split.size == 2) { "Ugylig periode $this."}
            return Periode(
                fom = when (split[0]) {
                    Åpen -> null
                    else -> LocalDate.parse(split[0])
                },
                tom = when (split[1]) {
                    Åpen -> null
                    else -> LocalDate.parse(split[1])
                }
            )
        }
        internal fun LocalDate.somPeriode() = Periode(fom = this, tom = this)
        internal fun Periode.forsikreLukketPeriode() = when {
            fom != null && tom != null -> this
            fom != null -> fom.somPeriode()
            tom != null -> tom.somPeriode()
            else -> throw IllegalStateException("Må være satt minst fom eller tom for å lage en lukket periode.")
        }
    }
}
