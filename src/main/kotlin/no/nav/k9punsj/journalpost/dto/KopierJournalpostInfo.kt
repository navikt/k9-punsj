package no.nav.k9punsj.journalpost.dto

import no.nav.k9punsj.felles.PunsjFagsakYtelseType

data class KopierJournalpostInfo(
    val nyJournalpostId: String,
    val saksnummer: String,
    val fra: String,
    val til: String,
    val pleietrengende: String? = null,
    val annenPart: String? = null,
    val ytelse: PunsjFagsakYtelseType
)
