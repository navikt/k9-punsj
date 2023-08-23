package no.nav.k9punsj.integrasjoner.k9sak.dto

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.JournalpostId
import no.nav.k9punsj.felles.Periode
import no.nav.k9punsj.felles.Periode.Companion.ÅpenPeriode
import no.nav.k9punsj.felles.Søknadstype
import java.time.ZonedDateTime

data class PunsjetSøknad(
    internal val versjon: String,
    internal val søknadId: String,
    internal val saksnummer: String?,
    internal val søknadstype: Søknadstype,
    internal val søker: Identitetsnummer,
    internal val pleietrengende: Identitetsnummer?,
    internal val annenPart: Identitetsnummer?,
    internal val journalpostIder: Set<JournalpostId>,
    internal val periode: Periode,
    internal val mottatt: ZonedDateTime,
    internal val søknadJson: ObjectNode,
    internal val saksbehandler: String) {
    internal val identitetsnummer = setOfNotNull(søker, pleietrengende, annenPart)
    init {
        require(identitetsnummer.isNotEmpty()) { "Søknaden må gjelde minst en person." }
        require(journalpostIder.isNotEmpty()) { "Søknaden må være knyttet til minst en journalpostId."}
        require(listOfNotNull(søker, pleietrengende, annenPart).size == identitetsnummer.size) {
            "Søknaden må ha unike personer som søker/pleietrengende/annenPart."
        }
    }
}