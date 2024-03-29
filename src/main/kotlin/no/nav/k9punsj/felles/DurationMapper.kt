package no.nav.k9punsj.felles

import no.nav.k9punsj.felles.dto.TimerOgMinutter
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import java.time.Duration
import kotlin.math.roundToLong

internal object DurationMapper {
    internal fun String?.somDuration(): Duration? {
        if (isNullOrBlank()) return null

        // Om man oppgir gyldig ISO-standard
        kotlin.runCatching { Duration.parse(this) }.onSuccess { return it }

        // Om man oppgir et heltal antall timer
        val heltall = toLongOrNull()
        if (heltall != null) {
            return Duration.ofHours(heltall)
        }

        // Om man oppgir et desimaltall med enten '.' eller ','  5.5 == 5 timer og 30 minutter
        val desimal = somDesimalOrNull()
        if (desimal != null) {
            val millis = (desimal * EnTimeInMillis).roundToLong()
            return Duration.ofMillis(millis).rundTilNærmesteTimerOgMinutter()
        }

        // Om man oppgir <timer>:<minutter> 5:30 == 5 timer og 30 minutter
        val timerOgMinutter = somTimerOgMinutterOrNull()
        if (timerOgMinutter != null) {
            val (timer, minutter) = timerOgMinutter
            return Duration.ofHours(timer).plusMinutes(minutter)
        }

        // Ikke en støttet måte å oppgi tid på
        throw IllegalArgumentException("Ugyldig tid $this")
    }

    private fun Duration.rundTilNærmesteTimerOgMinutter(): Duration {
        val nøyaktig = Duration.ofNanos(toNanos())
        val sekunder = nøyaktig.toSecondsPart().toLong()
        return nøyaktig
            .minusSeconds(sekunder)
            .minusNanos(nøyaktig.toNanosPart().toLong())
            .let { if (sekunder >= 30) it.plusMinutes(1) else it }
    }

    internal fun Duration.somTimerOgMinutter(): Pair<Long, Int> {
        val avrundet = rundTilNærmesteTimerOgMinutter()
        return avrundet.toHours() to avrundet.toMinutesPart()
    }

    /*
        Korrigerer arbeidstid som havner på 80% fordi att saken får avslag om faktisk arbeidstid er øver 80% (80.1% gir avslag f.eks.)
     */
    internal fun korrigereArbeidstidRettOver80Prosent(
        faktiskArbeidTimerPerDag: String?,
        jobberNormaltTimerPerDag: String?
    ): TimerOgMinutter? {
        if (!faktiskArbeidTimerPerDag.isNullOrEmpty() && !jobberNormaltTimerPerDag.isNullOrEmpty()) {
            val beregnetArbeidstidProsent = faktiskArbeidTimerPerDag.somDuration()?.multipliedBy(100)
                ?.dividedBy(jobberNormaltTimerPerDag.somDuration())
            val korrigertArbeidstid = if (beregnetArbeidstidProsent!! == 80L) {
                faktiskArbeidTimerPerDag.somDuration()?.minusMinutes(1)
            } else {
                faktiskArbeidTimerPerDag.somDuration()
            }
            return korrigertArbeidstid?.somTimerOgMinutter().somTimerOgMinutterDto()
        }
        return null
    }

    private fun String.somDesimalOrNull() = replace(",", ".").toDoubleOrNull()

    private fun String.somTimerOgMinutterOrNull(): Pair<Long, Long>? {
        if (split(":").size != 2) return null
        val timer = split(":")[0].toLongOrNull() ?: return null
        val minutter = split(":")[1].toLongOrNull() ?: return null
        if (timer < 0 || minutter < 0 || minutter > 60) return null
        return timer to minutter
    }

    private val EnTimeInMillis = Duration.ofHours(1).toMillis()
}
