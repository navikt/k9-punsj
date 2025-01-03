package no.nav.k9punsj.felles

import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.omsorgspengeraleneomsorg.utledDato
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Disabled("Baluba etter nyttår")
class PeriodeDtoTest {

    @Test
    fun `Hvis fom er tidligere enn 2 år siden, bruk starten av siste året`() {
        val periodeEldreEnn2År = PeriodeDto(
            fom = LocalDate.parse("2019-01-01"),
            tom = null
        ).utledDato()

        assertThat(periodeEldreEnn2År.fom).isEqualTo(LocalDate.parse("2023-01-01"))
    }

    @Test
    fun `Hvis fom er nøyaktig 2 år siden, bruk starten av siste året`() {
        val periodeNøyaktig2ÅrSiden = PeriodeDto(
            fom = LocalDate.parse("2021-01-01"),
            tom = null
        ).utledDato()

        assertThat(periodeNøyaktig2ÅrSiden.fom).isEqualTo(LocalDate.parse("2023-01-01"))
    }

    @Test
    fun `Hvis fom er ila de siste 2 årene, bruk fom`() {
        val periodeEldreEnn2År = PeriodeDto(
            fom = LocalDate.parse("2023-01-01"),
            tom = null
        ).utledDato()

        assertThat(periodeEldreEnn2År.fom).isEqualTo(LocalDate.parse("2023-01-01"))
    }

    @Test
    fun `Hvis fom er idag, bruk fom`() {
        val periodeEldreEnn2År = PeriodeDto(
            fom = LocalDate.parse("2023-06-20"),
            tom = null
        ).utledDato()

        assertThat(periodeEldreEnn2År.fom).isEqualTo(LocalDate.parse("2023-06-20"))
    }

    @Test
    fun `Hvis fom er null, returner samme objekt`() {
        val periodeNullFom = PeriodeDto(
            fom = null,
            tom = null
        ).utledDato()

        assertThat(periodeNullFom.fom).isNull()
    }
}
