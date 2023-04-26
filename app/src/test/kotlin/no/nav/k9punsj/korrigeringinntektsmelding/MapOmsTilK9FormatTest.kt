package no.nav.k9punsj.korrigeringinntektsmelding

import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.korrigeringinntektsmelding.MapOmsTilK9Format.Companion.somEnkeltDager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MapOmsTilK9FormatTest {

    @Test
    fun `should return list of single days between start and end dates`() {
        val startDate = LocalDate.of(2023, 4, 1)
        val endDate = LocalDate.of(2023, 4, 5)
        val expected = listOf(
            LocalDate.of(2023, 4, 1).toPeriodeDto(),
            LocalDate.of(2023, 4, 2).toPeriodeDto(),
            LocalDate.of(2023, 4, 3).toPeriodeDto(),
            LocalDate.of(2023, 4, 4).toPeriodeDto(),
            LocalDate.of(2023, 4, 5).toPeriodeDto()
        )

        val actual = PeriodeDto(startDate, endDate).somEnkeltDager()

        assertEquals(expected, actual)
    }

    @Test
    fun `handles null values`() {
        val startDate = LocalDate.of(2023, 4, 1)
        val result = PeriodeDto(startDate, null).somEnkeltDager()
        assert(result.isEmpty())
    }

    fun LocalDate.toPeriodeDto() = PeriodeDto(this, this)
}