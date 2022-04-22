package no.nav.k9punsj.pleiepengersyktbarn

import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.felles.dto.PeriodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class MapPsbTilK9FormatTest {

    @Test
    fun `tom søknad med komplett struktur skal gi feil fra k9-format`() {
        val periode =
            PeriodeDto(LocalDate.now(), LocalDate.now().plusMonths(1))

        PleiepengerSøknadVisningDtoUtils.søknadMedKomplettStruktur(requiredPeriode = periode,optionalPeriode = null)
            .feil()
            .assertInneholderFeil()

        PleiepengerSøknadVisningDtoUtils.søknadMedKomplettStruktur(requiredPeriode = periode,optionalPeriode = periode)
            .feil()
            .assertInneholderFeil()
    }

    private fun PleiepengerSyktBarnSøknadDto.feil() = MapPsbTilK9Format(
        dto = this,
        perioderSomFinnesIK9 = emptyList(),
        journalpostIder = setOf("123456789"),
        søknadId = "${UUID.randomUUID()}"
    ).feil()

    private fun List<Feil>.assertInneholderFeil() {
        assertThat(this).isNotEmpty
        assertThat(this.filter { it.feilkode == "uventetMappingfeil" }).isEmpty()
    }
}
