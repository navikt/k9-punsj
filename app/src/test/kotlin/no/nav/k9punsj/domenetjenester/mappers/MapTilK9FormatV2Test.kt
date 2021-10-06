package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import no.nav.k9punsj.util.PleiepengerSøknadVisningDtoUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class MapTilK9FormatV2Test {

    @Test
    fun `tom søknad med komplett struktur skal gi feil fra k9-format`() {
        val periode = PeriodeDto(LocalDate.now(), LocalDate.now().plusMonths(1))
        PleiepengerSøknadVisningDtoUtils.søknadMedKomplettStruktur(
            requiredPeriode = periode,
            optionalPeriode = null
        ).søknadOgFeil().second.assertInneholderFeil()
        PleiepengerSøknadVisningDtoUtils.søknadMedKomplettStruktur(
            requiredPeriode = periode,
            optionalPeriode = listOf(periode)
        ).søknadOgFeil().second.assertInneholderFeil()
    }

    private fun PleiepengerSøknadVisningDto.søknadOgFeil() = MapTilK9FormatV2(
        dto = this,
        perioderSomFinnesIK9 = emptyList(),
        journalpostIder = setOf("123456789"),
        søknadId = "${UUID.randomUUID()}"
    ).søknadOgFeil()

    private fun List<Feil>.assertInneholderFeil() {
        assertThat(this).isNotEmpty
        assertThat(this.filter { it.feilkode == "uventetMappingfeil" }).isEmpty()
    }
}