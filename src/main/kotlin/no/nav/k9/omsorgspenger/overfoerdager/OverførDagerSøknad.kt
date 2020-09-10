package no.nav.k9.omsorgspenger.overfoerdager

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

enum class JaNei { ja, nei }

enum class Mottaker { Ektefelle, Samboer }

data class Avsender (
        val fødselsnummer: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidssituasjon (
        val erArbeidstaker: Boolean?,
        val erFrilanser: Boolean?,
        val erSelvstendigNæringsdrivende: Boolean?
)

data class Barn (
        val fødselsnummer: String
)

data class OmsorgenDelesMed (
        val fødselsnummer: String,
        val mottaker: Mottaker,
        val antallOverførteDager: Int,
        val samboerSiden: LocalDate?
)

data class OverførDagerSøknad (
        val mottaksdato: LocalDate,
        val avsender: Avsender,
        val arbeidssituasjon: Arbeidssituasjon,
        val aleneOmOmsorgen: JaNei,
        val barn: List<Barn>,
        val omsorgenDelesMed: OmsorgenDelesMed
)

data class OverførDagerDTO (
        val journalpostId: String,
        val søknad: OverførDagerSøknad
)
