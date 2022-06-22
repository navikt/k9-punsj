package no.nav.k9punsj.omsorgspenger.overfoerdager

import de.huxhorn.sulky.ulid.ULID
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OverførDagerConverterTest {

    @Test
    internal fun `Mapper overføringsskjema fra webformat til internformat`() {
        val etBarn = Barn(
            norskIdent = "01010101010",
            fødselsdato = LocalDate.now().minusYears(10)
        )
        val mottaksdato = LocalDate.now()
        val søknad = OverførDagerSøknad(
            mottaksdato = mottaksdato,
            norskIdent = "12121212121",
            arbeidssituasjon = Arbeidssituasjon(
                erArbeidstaker = true,
                erFrilanser = null,
                erSelvstendigNæringsdrivende = false
            ),
            borINorge = JaNei.ja,
            aleneOmOmsorgen = JaNei.ja,
            barn = listOf(etBarn, etBarn, etBarn),
            omsorgenDelesMed = OmsorgenDelesMed(
                norskIdent = "10101010101",
                mottaker = Mottaker.Samboer,
                antallOverførteDager = 7,
                samboerSiden = LocalDate.now().minusYears(2)
            )
        )
        val journalpostId = "123"
        val overførDagerDTO = OverførDagerDTO(
            søknad = søknad,
            journalpostIder = listOf(journalpostId),
            dedupKey = ULID().nextValue()
        )

        val mappet = OverførDagerConverter.map(overførDagerDTO)

        assertThat(mappet.fra.identitetsnummer).isEqualTo(søknad.norskIdent)
        assertThat(mappet.fra.jobberINorge).isTrue
        assertThat(mappet.til.harBoddSammenMinstEttÅr).isTrue
        assertThat(mappet.til.identitetsnummer).isEqualTo(søknad.omsorgenDelesMed.norskIdent)
        assertThat(mappet.til.relasjon).isEqualTo(OverføreOmsorgsdagerBehov.Relasjon.NåværendeSamboer)
        assertThat(mappet.omsorgsdagerÅOverføre).isEqualTo(søknad.omsorgenDelesMed.antallOverførteDager)
        assertThat(mappet.omsorgsdagerTattUtIÅr).isEqualTo(0)
        assertThat(mappet.barn).hasSize(3)
        assertThat(mappet.barn).allMatch {
            it.aleneOmOmsorgen &&
                !it.utvidetRett &&
                it.fødselsdato.isEqual(etBarn.fødselsdato) &&
                it.identitetsnummer == etBarn.norskIdent
        }
        assertThat(mappet.journalpostIder).hasSize(1)
        assertThat(mappet.journalpostIder[0]).isEqualTo(journalpostId)
        assertThat(mappet.mottatt.toLocalDate()).isEqualTo(mottaksdato)
    }
}
