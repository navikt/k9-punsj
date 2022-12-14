package no.nav.k9punsj.opplaeringspenger

import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.ArbeidstidDto
import no.nav.k9punsj.felles.dto.BostederDto
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.UtenlandsoppholdDto
import no.nav.k9punsj.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

internal class MapOlpTilK9FormatTest {

    @Test
    fun `tom søknad med komplett struktur skal gi feil fra k9-format`() {
        val periode =
            PeriodeDto(LocalDate.now(), LocalDate.now().plusMonths(1))

        OpplaeringspengerSoknadVisningDtoUtils.søknadMedKomplettStruktur(requiredPeriode = periode, optionalPeriode = null)
            .feil()
            .assertInneholderFeil()

        OpplaeringspengerSoknadVisningDtoUtils.søknadMedKomplettStruktur(requiredPeriode = periode, optionalPeriode = periode)
            .feil()
            .assertInneholderFeil()
    }

    private fun OpplaeringspengerSøknadDto.feil() = MapOlpTilK9Format(
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
