package no.nav.k9punsj.journalpost.dto

import no.nav.k9.kodeverk.behandling.FagsakYtelseType

data class KopierJournalpostInfo(
    internal val journalpostId: String,
    internal val fra: String,
    internal val til: String,
    internal val pleietrengende: String? = null,
    internal val annenPart: String? = null,
    internal val ytelse: FagsakYtelseType
)
