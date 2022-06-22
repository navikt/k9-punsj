package no.nav.k9punsj.omsorgspengermidlertidigalene

import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.AnnenForelder.SituasjonType
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerMidlertidigAlene
import no.nav.k9punsj.felles.dto.PeriodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

internal class MapOmsMATilK9FormatTest {

    @Test
    fun `Forvent korrekt mapping`() {
        val søknadId = UUID.randomUUID().toString()
        val (søknad, feil) = MapOmsMATilK9Format(
            søknadId = søknadId,
            journalpostIder = setOf(),
            dto = OmsorgspengerMidlertidigAleneSøknadDto(
                soeknadId = søknadId,
                soekerId = "11111111111",
                mottattDato = LocalDate.now(),
                klokkeslett = LocalTime.now(),
                barn = listOf(
                    OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(norskIdent = "22222222222", foedselsdato = null),
                    OmsorgspengerMidlertidigAleneSøknadDto.BarnDto(norskIdent = "33333333333", foedselsdato = null)
                ),
                annenForelder = OmsorgspengerMidlertidigAleneSøknadDto.AnnenForelder(
                    norskIdent = "44444444444",
                    situasjonType = "FENGSEL",
                    situasjonBeskrivelse = "beskrivelse",
                    periode = PeriodeDto(
                        fom = LocalDate.now().minusDays(5),
                        tom = LocalDate.now().plusDays(5)
                    )
                ),
                journalposter = listOf(),
                harInfoSomIkkeKanPunsjes = true,
                harMedisinskeOpplysninger = true
            )
        ).søknadOgFeil()

        assertThat(feil).isEmpty()
        val ytelse = søknad.getYtelse<OmsorgspengerMidlertidigAlene>()
        assertThat(ytelse.barn).size().isEqualTo(2)
        assertThat(ytelse.barn.first().personIdent.verdi).isEqualTo("22222222222")
        assertThat(ytelse.annenForelder.personIdent.verdi).isEqualTo("44444444444")
        assertThat(ytelse.annenForelder.situasjonType).isEqualTo(SituasjonType.FENGSEL)
        assertThat(ytelse.annenForelder.situasjonBeskrivelse).isEqualTo("beskrivelse")
        assertThat(ytelse.annenForelder.periode).isEqualTo(
            Periode(
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(5)
            )
        )
    }
}
