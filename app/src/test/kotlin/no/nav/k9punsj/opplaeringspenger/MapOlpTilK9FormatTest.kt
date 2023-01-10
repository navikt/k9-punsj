package no.nav.k9punsj.opplaeringspenger

import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.felles.dto.PeriodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
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

    @Test
    fun `Kurs med flere kursperioder utleder søknadsperiode fra første o siste dato i perioderna`() {
        val periode1 = KursPeriode(
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 1, 6)
        )
        val periode2 = KursPeriode(
            LocalDate.of(2023, 1, 7),
            LocalDate.of(2023, 1, 14)
        )
        val periode3 = KursPeriode(
            LocalDate.of(2023, 1, 15),
            LocalDate.of(2023, 1, 29)
        )

        val kurs = OpplaeringspengerSøknadDto.Kurs(
            kursHolder = OpplaeringspengerSøknadDto.KursHolder(holder = "test", institusjonsUuid = null),
            kursperioder = listOf(periode3, periode1, periode2)
        )

        val søknadsperiode = kurs.utledsSoeknadsPeriodeFraAvreiseOgHjemkomstDatoer()

        assert(søknadsperiode != null)
        assert(søknadsperiode!!.fom == LocalDate.of(2023, 1, 1))
        assert(søknadsperiode!!.tom == LocalDate.of(2023, 1, 29))
    }

    private fun KursPeriode(fom: LocalDate, tom: LocalDate) = OpplaeringspengerSøknadDto.KursPeriodeMedReisetid(
            periode = PeriodeDto(
                fom = fom,
                tom = tom
            ),
            avreise = fom,
            hjemkomst = tom,
            begrunnelseReisetidHjem = "test",
            begrunnelseReisetidTil = "test",
        )


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
