package no.nav.k9punsj.felles.k9format

import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.TimerOgMinutter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

internal class MapArbeidstidTest {

    private val fom = LocalDate.parse("2024-01-01")
    private val tom = LocalDate.parse("2024-01-31")
    private val periode = PeriodeDto(fom, tom)
    private val k9Periode = Periode(fom, tom)

    private fun mapArbeidstid(
        faktiskArbeidTimerPerDag: String? = null,
        jobberNormaltTimerPerDag: String? = null,
        fraværTimerPerDag: String? = null,
        støtterFravær: Boolean = false
    ): Pair<ArbeidstidInfo?, List<Feil>> {
        val feil = mutableListOf<Feil>()
        val dto = ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
            perioder = listOf(
                ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                    periode = periode,
                    faktiskArbeidTimerPerDag = faktiskArbeidTimerPerDag,
                    jobberNormaltTimerPerDag = jobberNormaltTimerPerDag,
                    fraværTimerPerDag = fraværTimerPerDag
                )
            )
        )
        return dto.mapArbeidstid("test", feil, støtterFravær) to feil
    }

    @Test
    fun `normal arbeidstid mappes direkte til k9-format`() {
        val (info, feil) = mapArbeidstid(faktiskArbeidTimerPerDag = "7,5", jobberNormaltTimerPerDag = "7,5")

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ofMinutes(450))
        assertThat(periodeInfo.jobberNormaltTimerPerDag).isEqualTo(Duration.ofMinutes(450))
    }

    @Test
    fun `delvis arbeidstid mappes direkte til k9-format`() {
        val (info, feil) = mapArbeidstid(faktiskArbeidTimerPerDag = "4", jobberNormaltTimerPerDag = "7,5")

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ofHours(4))
        assertThat(periodeInfo.jobberNormaltTimerPerDag).isEqualTo(Duration.ofMinutes(450))
    }

    @Test
    fun `faktisk arbeidstid i desimal konverteres korrekt til timer og minutter`() {
        val (info, feil) = mapArbeidstid(faktiskArbeidTimerPerDag = "4,5", jobberNormaltTimerPerDag = "7,5")

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ofMinutes(270))
        assertThat(periodeInfo.jobberNormaltTimerPerDag).isEqualTo(Duration.ofMinutes(450))
    }

    @Test
    fun `faktisk arbeidstid som TimerOgMinutter-objekt mappes korrekt`() {
        val feil = mutableListOf<Feil>()
        val dto = ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
            perioder = listOf(
                ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                    periode = periode,
                    faktiskArbeidTimerPerDag = null,
                    jobberNormaltTimerPerDag = null,
                    faktiskArbeidPerDag = TimerOgMinutter(4, 30),
                    jobberNormaltPerDag = TimerOgMinutter(7, 30)
                )
            )
        )
        val info = dto.mapArbeidstid("test", feil)

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ofMinutes(270))
        assertThat(periodeInfo.jobberNormaltTimerPerDag).isEqualTo(Duration.ofMinutes(450))
    }

    @Test
    fun `fravær i desimal beregner faktisk arbeidstid som normalt minus fravær`() {
        val (info, feil) = mapArbeidstid(
            jobberNormaltTimerPerDag = "7,5",
            fraværTimerPerDag = "3,5",
            støtterFravær = true
        )

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.jobberNormaltTimerPerDag).isEqualTo(Duration.ofMinutes(450))
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ofHours(4))
    }

    @Test
    fun `fravær hele dagen gir faktisk arbeidstid 0`() {
        val (info, feil) = mapArbeidstid(
            jobberNormaltTimerPerDag = "7,5",
            fraværTimerPerDag = "7,5",
            støtterFravær = true
        )

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `fravær større enn normaltid gir faktisk arbeidstid 0 og ikke negativ`() {
        val (info, feil) = mapArbeidstid(
            jobberNormaltTimerPerDag = "7,5",
            fraværTimerPerDag = "8",
            støtterFravær = true
        )

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `fravær oppgitt som timer og minutter beregner faktisk arbeidstid korrekt`() {
        val (info, feil) = mapArbeidstid(
            jobberNormaltTimerPerDag = "7:30",
            fraværTimerPerDag = "3:30",
            støtterFravær = true
        )

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.jobberNormaltTimerPerDag).isEqualTo(Duration.ofMinutes(450))
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ofHours(4))
    }

    @Test
    fun `fravær uten normaltid gir valideringsfeil`() {
        val (_, feil) = mapArbeidstid(fraværTimerPerDag = "3,5", støtterFravær = true)

        assertThat(feil).anyMatch { it.feilkode == "fraværUtenNormaltid" }
    }

    @Test
    fun `fravær og faktisk arbeidstid samtidig gir valideringsfeil`() {
        val (_, feil) = mapArbeidstid(
            faktiskArbeidTimerPerDag = "4",
            jobberNormaltTimerPerDag = "7,5",
            fraværTimerPerDag = "3,5",
            støtterFravær = true
        )

        assertThat(feil).anyMatch { it.feilkode == "fraværOgFaktiskArbeidSamtidig" }
    }

    @Test
    fun `fravær ignoreres når støtterFravær er false`() {
        val (info, feil) = mapArbeidstid(
            faktiskArbeidTimerPerDag = "4",
            jobberNormaltTimerPerDag = "7,5",
            fraværTimerPerDag = "3,5",
            støtterFravær = false
        )

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ofHours(4))
        assertThat(periodeInfo.jobberNormaltTimerPerDag).isEqualTo(Duration.ofMinutes(450))
    }

    @Test
    fun `fravær som TimerOgMinutter-objekt beregner faktisk arbeidstid korrekt`() {
        val feil = mutableListOf<Feil>()
        val dto = ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
            perioder = listOf(
                ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                    periode = periode,
                    faktiskArbeidTimerPerDag = null,
                    jobberNormaltTimerPerDag = "7,5",
                    fraværTimerPerDag = null,
                    fraværPerDag = TimerOgMinutter(3, 30)
                )
            )
        )
        val info = dto.mapArbeidstid("test", feil, støtterFravær = true)

        assertThat(feil).isEmpty()
        val periodeInfo = info!!.perioder[k9Periode]!!
        assertThat(periodeInfo.jobberNormaltTimerPerDag).isEqualTo(Duration.ofMinutes(450))
        assertThat(periodeInfo.faktiskArbeidTimerPerDag).isEqualTo(Duration.ofHours(4))
    }
}
