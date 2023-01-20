package no.nav.k9punsj.util

import no.nav.k9punsj.felles.DurationMapper.korrigereArbeidstidRettOver80Prosent
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.dto.TimerOgMinutter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

internal class DurationMapperTest {

    @Test
    fun `mapping av tid fra optional string`() {
        assertThat(setOf(null.somDuration(), "".somDuration(), " ".somDuration())).hasSameElementsAs(setOf(null))
        assertThat(setOf("4".somDuration(), "4.0".somDuration(), "4,0".somDuration(), "4:00".somDuration(), "PT4H".somDuration())).hasSameElementsAs(setOf(Duration.ofHours(4)))
        assertThat(setOf("5.75".somDuration(), "5,75".somDuration(), "5:45".somDuration(), "PT5H45M".somDuration())).hasSameElementsAs(setOf(Duration.ofHours(5).plusMinutes(45)))
        assertThat(setOf("0".somDuration(), "0:0".somDuration(), "PT0S".somDuration())).hasSameElementsAs(setOf(Duration.ofSeconds(0)))
        assertThrows<IllegalArgumentException> { "5:61".somDuration() }
        assertThrows<IllegalArgumentException> { "5:".somDuration() }
        // Blir egentlig 5 timer, 52 minutter og 48 sekunder. Runder opp et minutt >=30
        assertEquals("5,88".somDuration(), Duration.ofHours(5).plusMinutes(53))
        // Blir egentlig 5 timer, 51 minutter og 18 sekunder. Sekundene strykes
        assertEquals("5,855".somDuration(), Duration.ofHours(5).plusMinutes(51))
    }

    @Test
    fun `mapping fra duration til timer og minutter`() {
        val duration = Duration.ofDays(7)
            .plusHours(13)
            .plusMinutes(26)
            .plusSeconds(30) // Runder opp til 27 min
            .plusMillis(999) // Strykes
            .plusNanos(1) // Strykes

        val forventet = ((7 * 24) + 13).toLong() to 27
        assertEquals(forventet, duration.somTimerOgMinutter())
    }

    @Test
    fun `beregnet arbeidstid 80 prosent avrunder faktisktArbeidstid ned 1min så man ikke går over 80 med en decimal`() {
        val faktiskArbeidTimerPerDag = "6,88" // // 80% = 53min
        val jobberNormaltTimerPerDag = "8,6"

        val forventet = TimerOgMinutter(timer = 6, minutter = 52)
        val faktiskt = korrigereArbeidstidRettOver80Prosent(faktiskArbeidTimerPerDag, jobberNormaltTimerPerDag)
        assertEquals(forventet, faktiskt)
    }

    @Test
    fun `avrunder ikke faktisktArbeidstid ned dersom beregnet arbeidstid er under eller over 80 prosent`() {

        val jobberNormaltTimerPerDag = "8,6"

        val faktiskArbeidTimerPerDag1 = "6,87" // 79.884% = 52min
        val forventet1 = TimerOgMinutter(timer = 6, minutter = 52)
        val faktiskt1 = korrigereArbeidstidRettOver80Prosent(faktiskArbeidTimerPerDag1, jobberNormaltTimerPerDag)
        assertEquals(forventet1, faktiskt1)

        val faktiskArbeidTimerPerDag2 = "6,97" // 81.047% = 58min
        val forventet2 = TimerOgMinutter(timer = 6, minutter = 58)
        val faktiskt2 = korrigereArbeidstidRettOver80Prosent(faktiskArbeidTimerPerDag2, jobberNormaltTimerPerDag)
        assertEquals(forventet2, faktiskt2)
    }
}
