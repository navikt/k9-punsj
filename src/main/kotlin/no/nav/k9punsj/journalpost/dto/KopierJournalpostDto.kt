package no.nav.k9punsj.journalpost.dto

import no.nav.k9punsj.felles.PunsjFagsakYtelseType

data class KopierJournalpostDto(
    val fra: String,
    val til: String,
    val barn: String?,
    val annenPart: String?,
    val ytelse: PunsjFagsakYtelseType?
) {
    init {
        require(barn != null || annenPart != null) {
            "Må sette minst en av barn og annenPart"
        }
    }
}
