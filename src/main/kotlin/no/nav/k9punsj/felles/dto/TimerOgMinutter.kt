package no.nav.k9punsj.felles.dto

import java.time.Duration

data class TimerOgMinutter(
    val timer: Long,
    val minutter: Int
) {
    internal companion object {
        fun Pair<Long, Int>?.somTimerOgMinutterDto() = when (this) {
            null -> null
            else -> TimerOgMinutter(first, second)
        }

        fun TimerOgMinutter.somDuration() = Duration.ofHours(timer).plusMinutes(minutter.toLong())
    }
}
