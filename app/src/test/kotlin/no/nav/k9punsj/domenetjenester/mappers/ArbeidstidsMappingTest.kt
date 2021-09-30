package no.nav.k9punsj.domenetjenester.mappers

import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidPeriodeInfo
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

internal class ArbeidstidsMappingTest {

    @Test
    fun `oppgi perioder og dager for arbeidstid`() {
        arbeidstidPerioderOgDager(
            aktiv = PleiepengerSøknadVisningDto.AktivtInterval.perioder
        ).assertArbeidstidPeriodeInfo(forventet = mapOf(
            arbeidstidperiode to (Duration.ofHours(7).plusMinutes(30) to Duration.ofHours(5))
        ))

        arbeidstidPerioderOgDager(
            aktiv = PleiepengerSøknadVisningDto.AktivtInterval.dager
        ).assertArbeidstidPeriodeInfo(forventet = mapOf(
            arbeidstiddag.somEnkeltdagPeriode() to (Duration.ofHours(8) to Duration.ofHours(5).plusMinutes(30))
        ))

        arbeidstidPerioderOgDager(
            aktiv = PleiepengerSøknadVisningDto.AktivtInterval.begge
        ).assertArbeidstidPeriodeInfo(forventet = mapOf(
            arbeidstidperiode to (Duration.ofHours(7).plusMinutes(30) to Duration.ofHours(5)),
            arbeidstiddag.somEnkeltdagPeriode() to (Duration.ofHours(8) to Duration.ofHours(5).plusMinutes(30))
        ))
    }

    private companion object {
        private val søknadsperiode = PeriodeDto(LocalDate.now(), LocalDate.now().plusMonths(1))
        private val arbeidstidperiode = søknadsperiode.copy(tom = søknadsperiode.tom!!.minusDays(1))
        private val arbeidstiddag = søknadsperiode.tom!!

        private fun arbeidstidPerioderOgDager(
            aktiv: PleiepengerSøknadVisningDto.AktivtInterval
        ) : Map<Periode, ArbeidstidPeriodeInfo> {
            @Language("JSON")
            val json = """
            {
                "arbeidstakerList": [{
                    "arbeidstidInfo": {
                        "perioder": [{
                            "periode": {
                                "fom": "${arbeidstidperiode.fom}",
                                "tom": "${arbeidstidperiode.tom}"
                            },
                            "jobberNormaltTimerPerDag": "7,5",
                            "faktiskArbeidTimerPerDag": "5"
                        }],
                        "dager": [{
                            "dag": "$arbeidstiddag",
                            "jobberNormaltTimerPerDag": "8",
                            "faktiskArbeidTimerPerDag": "5,5"
                        }],
                        "aktiv": "$aktiv"
                    }
                }]
            }
            """.trimIndent()

            return PleiepengerSøknadVisningDtoUtils.minimalSøknadSomValiderer(
                søknadsperiode = søknadsperiode,
                appendJson = mapOf("arbeidstid" to json)
            ).søknadOgFeil().first.getYtelse<PleiepengerSyktBarn>().arbeidstid.arbeidstakerList.first().arbeidstidInfo.perioder
        }

        private fun Map<Periode, ArbeidstidPeriodeInfo>.assertArbeidstidPeriodeInfo(
            forventet: Map<PeriodeDto, Pair<Duration, Duration>>) {
            assertThat(this.keys).hasSameElementsAs(forventet.keys.map { it.somK9Periode() })
            forventet.forEach { periode, (normalt, faktisk) ->
                val periodeInfo = get(periode.somK9Periode())!!
                assertEquals(normalt, periodeInfo.jobberNormaltTimerPerDag)
                assertEquals(faktisk, periodeInfo.faktiskArbeidTimerPerDag)
            }
        }
    }
}