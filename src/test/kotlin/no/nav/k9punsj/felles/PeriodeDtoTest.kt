package no.nav.k9punsj.felles

import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.omsorgspengeraleneomsorg.utledDato
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PeriodeDtoTest {

    @Test
    fun `Hvis fom er tidligere enn 2 år siden, bruk starten av siste året`() {
        val periodeEldreEnn2År = PeriodeDto(
            fom = LocalDate.parse("2019-01-01"),
            tom = null
        ).utledDato()

        val forventetDato = LocalDate.now().minusYears(1).withMonth(1).withDayOfMonth(1)
        assertThat(periodeEldreEnn2År.fom).isEqualTo(forventetDato)
    }

    @Test
    fun `Hvis fom er nøyaktig 2 år siden, bruk starten av siste året`() {
        val periodeNøyaktig2ÅrSiden = PeriodeDto(
            fom = LocalDate.parse("2021-01-01"),
            tom = null
        ).utledDato()

        val forventetDato = LocalDate.now().minusYears(1).withMonth(1).withDayOfMonth(1)
        assertThat(periodeNøyaktig2ÅrSiden.fom).isEqualTo(forventetDato)
    }

    @Test
    fun `Hvis fom er ila de siste 2 årene, bruk fom`() {
        val periodeEldreEnn2År = PeriodeDto(
            fom = LocalDate.parse("2025-01-01"),
            tom = null
        ).utledDato()

        assertThat(periodeEldreEnn2År.fom).isEqualTo(LocalDate.parse("2025-01-01"))
    }

    @Test
    fun `Hvis fom er idag, bruk fom`() {
        val idag = LocalDate.now()
        val periodeEldreEnn2År = PeriodeDto(
            fom = idag,
            tom = null
        ).utledDato()

        assertThat(periodeEldreEnn2År.fom).isEqualTo(idag)
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
