package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9FormatV2.Companion.somDuration
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import no.nav.k9punsj.util.PleiepengerSøknadVisningDtoUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class MapTilK9FormatV2Test {

    @Test
    fun `mapping av tid`() {
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

    @Test
    fun `tom søknad med komplett struktur skal gi feil fra k9-format`() {
        val periode = PeriodeDto(LocalDate.now(), LocalDate.now().plusMonths(1))
        PleiepengerSøknadVisningDtoUtils.søknadMedKomplettStruktur(
            requiredPeriode = periode,
            optionalPeriode = null
        ).søknadOgFeil().second.assertInneholderFeil()
        PleiepengerSøknadVisningDtoUtils.søknadMedKomplettStruktur(
            requiredPeriode = periode,
            optionalPeriode = periode
        ).søknadOgFeil().second.assertInneholderFeil()
    }

    internal companion object {
        internal fun PleiepengerSøknadVisningDto.søknadOgFeil() = MapTilK9FormatV2(
            dto = this,
            perioderSomFinnesIK9 = emptyList(),
            journalpostIder = setOf("123456789"),
            søknadId = "${UUID.randomUUID()}"
        ).søknadOgFeil()

        internal fun LocalDate.somEnkeltdagPeriode() = PeriodeDto(fom = this, tom = this)

        private fun List<Feil>.assertInneholderFeil() {
            assertThat(this).isNotEmpty
            assertThat(this.filter { it.feilkode == "uventetMappingfeil" }).isEmpty()
        }
    }
}