package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9FormatV2.Companion.somK9Periode
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9FormatV2Test.Companion.somEnkeltdagPeriode
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9FormatV2Test.Companion.søknadOgFeil
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import no.nav.k9punsj.util.PleiepengerSøknadVisningDtoUtils
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

internal class TilsynsordningMappingTest {

    @Test
    fun `oppgi perioder og dager for tilsynsordning`() {

        tilsynsordningPerioderOgDager(
            aktiv = PleiepengerSøknadVisningDto.AktivtInterval.perioder
        ).assertTilsynPeriodeInfo(forventet = mapOf(
            tilsynsordningperiode to Duration.ofHours(7).plusMinutes(32)
        ))

        tilsynsordningPerioderOgDager(
            aktiv = PleiepengerSøknadVisningDto.AktivtInterval.dager
        ).assertTilsynPeriodeInfo(forventet = mapOf(
            tilsysnordningdag1.somEnkeltdagPeriode() to Duration.ofHours(5).plusMinutes(33),
            tilsysnordningdag2.somEnkeltdagPeriode() to Duration.ofHours(4)
        ))

        tilsynsordningPerioderOgDager(
            aktiv = PleiepengerSøknadVisningDto.AktivtInterval.begge
        ).assertTilsynPeriodeInfo(forventet = mapOf(
            tilsynsordningperiode to Duration.ofHours(7).plusMinutes(32),
            tilsysnordningdag1.somEnkeltdagPeriode() to Duration.ofHours(5).plusMinutes(33),
            tilsysnordningdag2.somEnkeltdagPeriode() to Duration.ofHours(4)
        ))
    }

    private companion object {
        private val søknadsperiode = PeriodeDto(LocalDate.now(), LocalDate.now().plusMonths(1))
        private val tilsynsordningperiode = søknadsperiode.copy(tom = søknadsperiode.tom!!.minusDays(2))
        private val tilsysnordningdag1 = søknadsperiode.tom!!
        private val tilsysnordningdag2 = søknadsperiode.tom!!.minusDays(1)

        private fun tilsynsordningPerioderOgDager(
            aktiv: PleiepengerSøknadVisningDto.AktivtInterval
        ) : Map<Periode, TilsynPeriodeInfo> {
            @Language("JSON")
            val json = """
            {
                "perioder": [{
                    "periode": {
                        "fom": "${tilsynsordningperiode.fom}",
                        "tom": "${tilsynsordningperiode.tom}"
                    },
                    "timer": 7,
                    "minutter": 32
                }],
                "dager": [{
                    "dag": "$tilsysnordningdag1",
                    "timer": 5,
                    "minutter": 33
                },{
                    "dag": "$tilsysnordningdag2",
                    "timer": 4,
                    "minutter": 0
                }],
                "aktiv": "$aktiv"
            }
            """.trimIndent()

            return PleiepengerSøknadVisningDtoUtils.minimalSøknadSomValiderer(
                søknadsperiode = søknadsperiode,
                appendJson = mapOf("tilsynsordning" to json)
            ).søknadOgFeil().first.getYtelse<PleiepengerSyktBarn>().tilsynsordning.perioder
        }

        private fun Map<Periode, TilsynPeriodeInfo>.assertTilsynPeriodeInfo(
            forventet: Map<PeriodeDto, Duration>) {
            assertThat(this.keys).hasSameElementsAs(forventet.keys.map { it.somK9Periode() })
            forventet.forEach { (periode, tilsynPerDag) ->
                val periodeInfo = get(periode.somK9Periode())!!
                assertEquals(tilsynPerDag, periodeInfo.etablertTilsynTimerPerDag)
            }
        }
    }
}