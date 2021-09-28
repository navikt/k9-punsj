package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9punsj.domenetjenester.mappers.MapTilK9FormatV2.Companion.somDuration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

internal class MapTilK9FormatV2Test {

    @Test
    fun `test mapping av tid`() {
        assertThat(setOf(null.somDuration(), "".somDuration(), " ".somDuration())).hasSameElementsAs(setOf(null))
        assertThat(setOf("4".somDuration(),"4.0".somDuration(),"4,0".somDuration(), "4:00".somDuration(), "PT4H".somDuration())).hasSameElementsAs(setOf(Duration.ofHours(4)))
        assertThat(setOf("5.75".somDuration(), "5,75".somDuration(), "5:45".somDuration(), "PT5H45M".somDuration())).hasSameElementsAs(setOf(Duration.ofHours(5).plusMinutes(45)))
        assertThat(setOf("0".somDuration(), "0:0".somDuration(), "PT0S".somDuration())).hasSameElementsAs(setOf(Duration.ofSeconds(0)))
        assertThrows<IllegalArgumentException> { "5:61".somDuration() }
        assertThrows<IllegalArgumentException> { "5:".somDuration() }
        // Blir egentlig 5 timer, 52 minutter og 48 sekunder. Runder opp et minutt >=30
        assertEquals("5,88".somDuration(), Duration.ofHours(5).plusMinutes(53))
        // Blir egentlig 5 timer, 51 minutter og 18 sekunder. Sekundene strykes
        assertEquals("5,855".somDuration(), Duration.ofHours(5).plusMinutes(51))
    }
}