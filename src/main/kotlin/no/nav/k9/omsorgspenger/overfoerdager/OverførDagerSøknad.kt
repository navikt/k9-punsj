package no.nav.k9.omsorgspenger.overfoerdager

enum class JaNei { ja, nei }

enum class Mottaker { Ektefelle, Samboer }

data class Arbeidssituasjon (
        val erArbeidstaker: Boolean,
        val erFrilanser: Boolean,
        val erSelvstendigNæringsdrivende: Boolean
)

data class Fosterbarn (
        val harFosterbarn: JaNei,
        val fødselsnummer: String
)

data class OmsorgenDelesMed (
        val fødselsnummer: String,
        val mottaker: Mottaker
)

data class OverførDagerSøknad (
        val arbeidssituasjon: Arbeidssituasjon,
        val aleneOmOmsorgen: JaNei,
        val fosterbarn: Fosterbarn,
        val omsorgenDelesMed: OmsorgenDelesMed
)
