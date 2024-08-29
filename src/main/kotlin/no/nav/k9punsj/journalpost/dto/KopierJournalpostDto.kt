package no.nav.k9punsj.journalpost.dto

import no.nav.k9.kodeverk.behandling.FagsakYtelseType

data class KopierJournalpostDto(
    val fra: String,
    val til: String,
    val barn: String?,
    val annenPart: String?,
    val ytelse: FagsakYtelseType?
) {
    init {
        require(barn != null || annenPart != null) {
            "Må sette minst en av barn og annenPart"
        }
    }
}
