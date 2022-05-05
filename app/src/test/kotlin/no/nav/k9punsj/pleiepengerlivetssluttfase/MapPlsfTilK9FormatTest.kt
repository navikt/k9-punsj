package no.nav.k9punsj.pleiepengerlivetssluttfase

import no.nav.k9.søknad.felles.Feil
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.ArbeidstidDto
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.PleietrengendeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

internal class MapPlsfTilK9FormatTest {

    @Test
    internal fun `korrigering av arbeidstid innenfor eksisterende søknadsperider er OK`() {
        val soeknadId = UUID.randomUUID().toString()
        val eksisterendeSøknadsperioder = listOf(
            PeriodeDto(
                fom = LocalDate.parse("2022-03-27"),
                tom = LocalDate.parse("2022-03-30")
            )
        )
        val feil = MapPlsfTilK9Format(
            søknadId = soeknadId,
            journalpostIder = setOf(),
            perioderSomFinnesIK9 = eksisterendeSøknadsperioder,
            dto = PleiepengerLivetsSluttfaseSøknadDto(
                soeknadId = soeknadId,
                soekerId = "11111111111",
                mottattDato = LocalDate.now(),
                klokkeslett = LocalTime.now(),
                pleietrengende = PleietrengendeDto("22222222222"),
                arbeidstid = arbeidstid(eksisterendeSøknadsperioder.first()),
                begrunnelseForInnsending = BegrunnelseForInnsending().medBegrunnelseForInnsending("Fordi..."),
                harInfoSomIkkeKanPunsjes = true,
                harMedisinskeOpplysninger = true
            )
        ).feil()

        assertThat(feil).isEmpty()
    }

    @Test
    internal fun `korrigering av arbeidstid utenfor eksisterende søknadsperider gir valideringsfeil`() {
        val soeknadId = UUID.randomUUID().toString()
        val eksisterendeSøknadsperioder = listOf(
            PeriodeDto(
                fom = LocalDate.parse("2022-03-27"),
                tom = LocalDate.parse("2022-03-30")
            )
        )
        val feil = MapPlsfTilK9Format(
            søknadId = soeknadId,
            journalpostIder = setOf(),
            perioderSomFinnesIK9 = eksisterendeSøknadsperioder,
            dto = PleiepengerLivetsSluttfaseSøknadDto(
                soeknadId = soeknadId,
                soekerId = "11111111111",
                mottattDato = LocalDate.now(),
                klokkeslett = LocalTime.now(),
                pleietrengende = PleietrengendeDto("22222222222"),
                arbeidstid = arbeidstid(
                    PeriodeDto(
                        fom = LocalDate.parse("2022-04-27"),
                        tom = LocalDate.parse("2022-04-30")
                    )
                ),
                begrunnelseForInnsending = BegrunnelseForInnsending().medBegrunnelseForInnsending("Fordi..."),
                harInfoSomIkkeKanPunsjes = true,
                harMedisinskeOpplysninger = true
            )
        ).feil()

        assertThat(feil).size().isEqualTo(1)
        assertThat(feil.first()).isEqualTo(Feil(
            "ytelse.arbeidstid.arbeidstakerList[0].perioder",
            "ugyldigPeriode",
            "Perioden er utenfor gyldig interval. Gyldig interval: ([[2022-03-27, 2022-03-30]]), Ugyldig periode: 2022-04-27/2022-04-30"
        ))
    }

    private fun arbeidstid(periode: PeriodeDto) = ArbeidstidDto(
        arbeidstakerList = listOf(
            ArbeidAktivitetDto.ArbeidstakerDto(
                norskIdent = null,
                organisasjonsnummer = "926032925",
                arbeidstidInfo = ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto(
                    perioder = listOf(
                        ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto(
                            periode = periode,
                            faktiskArbeidTimerPerDag = "7,5",
                            jobberNormaltTimerPerDag = "7,5"
                        )
                    )
                )
            )
        ),
        frilanserArbeidstidInfo = null,
        selvstendigNæringsdrivendeArbeidstidInfo = null
    )
}
