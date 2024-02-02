package no.nav.k9punsj.journalpost

import no.nav.k9punsj.fordel.K9FordelType
import java.time.DayOfWeek
import java.time.LocalDateTime

internal class VirkedagerUtil {

    companion object {
        internal val MANDAG = DayOfWeek.MONDAY.value
        internal val TIRSDAG = DayOfWeek.TUESDAY.value
        internal val ONSDAG = DayOfWeek.WEDNESDAY.value
        internal val TORSDAG = DayOfWeek.THURSDAY.value
        internal val FREDAG = DayOfWeek.FRIDAY.value
        internal val LØRDAG = DayOfWeek.SATURDAY.value
        internal val SØNDAG = DayOfWeek.SUNDAY.value

        internal fun tilbakeStillToVirkedagerHvisDetKommerFraScanning(type: String?, mottattDato: LocalDateTime): LocalDateTime {
            if (type == null) {
                return mottattDato
            }
            if (K9FordelType.sjekkOmDetErScanning(type)) {
                return mottattDato.trekkFraToVirkeDager()
            }
            return mottattDato
        }
    }
}

// trekker fra 2 virkedager hvis journalposten kommer fra scanning - dvs. en papir søknad
private fun LocalDateTime.trekkFraToVirkeDager(): LocalDateTime {
    when (this.dayOfWeek.value) {
        VirkedagerUtil.MANDAG -> {
            return this.minusDays(4)
        }
        VirkedagerUtil.TIRSDAG -> {
            return this.minusDays(4)
        }
        VirkedagerUtil.ONSDAG -> {
            return this.minusDays(2)
        }
        VirkedagerUtil.TORSDAG -> {
            return this.minusDays(2)
        }
        VirkedagerUtil.FREDAG -> {
            return this.minusDays(2)
        }
        VirkedagerUtil.LØRDAG -> {
            return this.minusDays(2)
        }
        VirkedagerUtil.SØNDAG -> {
            return this.minusDays(3)
        }
    }
    throw IllegalStateException("kommer aldri hit")
}
