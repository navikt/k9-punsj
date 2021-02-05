package no.nav.k9punsj.omsorgspenger.delingavomsorgsdager

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import de.huxhorn.sulky.ulid.ULID
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9punsj.omsorgspenger.overfoerdager.Barn
import java.time.LocalDate

enum class JaNei { ja, nei }

enum class Mottaker { Ektefelle, Samboer }

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidssituasjon (
        val erArbeidstaker: Boolean?,
        val erFrilanser: Boolean?,
        val erSelvstendigNæringsdrivende: Boolean?
)

data class OmsorgenDelesMed (
        val identitetsnummer: NorskIdentitetsnummer,
        val mottaker: Mottaker,
        val antallOverførteDager: Int,
        val samboerSiden: LocalDate?
)

data class DelingAvOmsorgsdagerMelding (
        val mottaksdato: LocalDate,
        val identitetsnummer: NorskIdentitetsnummer,
        val arbeidssituasjon: Arbeidssituasjon,
        val borINorge: JaNei,
        val aleneOmOmsorgen: JaNei,
        val barn: List<Barn>,
        val omsorgenDelesMed: OmsorgenDelesMed
)

data class DelingAvOmsorgsdagerDTO (
        val journalpostIder: List<String>,
        val melding: DelingAvOmsorgsdagerMelding,
        val dedupKey: ULID.Value
)
