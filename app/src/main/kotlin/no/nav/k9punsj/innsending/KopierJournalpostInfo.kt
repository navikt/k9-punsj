package no.nav.k9punsj.innsending

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.journalpost.JournalpostId

internal data class KopierJournalpostInfo(
    internal val correlationId: CorrelationId,
    internal val journalpostId: JournalpostId,
    internal val fra: String,
    internal val til: String,
    internal val pleietrengende: String? = null,
    internal val annenPart: String? = null,
    internal val ytelse: FagsakYtelseType
)