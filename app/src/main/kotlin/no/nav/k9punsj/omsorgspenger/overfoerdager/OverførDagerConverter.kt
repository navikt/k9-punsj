package no.nav.k9punsj.omsorgspenger.overfoerdager

import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.k9punsj.omsorgspenger.delingavomsorgsdager.DelingAvOmsorgsdagerConverter.Companion.osloNå

internal class OverførDagerConverter {

    companion object {
        fun map(dto: OverførDagerDTO): OverføreOmsorgsdagerBehov {
            val (journalpostIder, søknad) = dto
            val (mottaksdato, identitetsnummer, arbeidssituasjon, _, aleneOmOmsorgen, barn, omsorgenDelesMed) = søknad
            val jobberINorge = listOf(arbeidssituasjon.erArbeidstaker, arbeidssituasjon.erFrilanser, arbeidssituasjon.erSelvstendigNæringsdrivende)
                    .any { it == true }

            return OverføreOmsorgsdagerBehov(
                    fra = OverføreOmsorgsdagerBehov.OverførerFra(
                            identitetsnummer = identitetsnummer.toString(),
                            jobberINorge = jobberINorge
                    ),
                    til = OverføreOmsorgsdagerBehov.OverførerTil(
                            identitetsnummer = omsorgenDelesMed.norskIdent,
                            relasjon = when (omsorgenDelesMed.mottaker) {
                                Mottaker.Samboer -> OverføreOmsorgsdagerBehov.Relasjon.NåværendeSamboer
                                Mottaker.Ektefelle -> OverføreOmsorgsdagerBehov.Relasjon.NåværendeEktefelle
                            },
                            harBoddSammenMinstEttÅr = when (omsorgenDelesMed.mottaker) {
                                Mottaker.Samboer -> mottaksdato.isAfter(omsorgenDelesMed.samboerSiden?.plusYears(1))
                                Mottaker.Ektefelle -> null
                            }
                    ),
                    omsorgsdagerTattUtIÅr = 0,
                    omsorgsdagerÅOverføre = omsorgenDelesMed.antallOverførteDager,
                    barn = barn.map {
                        OverføreOmsorgsdagerBehov.Barn(
                                identitetsnummer = it.norskIdent,
                                fødselsdato = it.fødselsdato!!,
                                aleneOmOmsorgen = aleneOmOmsorgen == JaNei.ja,
                                utvidetRett = false
                        )
                    },
                    kilde = OverføreOmsorgsdagerBehov.Kilde.Brev,
                    journalpostIder = journalpostIder,
                    mottatt = mottaksdato.osloNå()
            )
        }
    }
}
