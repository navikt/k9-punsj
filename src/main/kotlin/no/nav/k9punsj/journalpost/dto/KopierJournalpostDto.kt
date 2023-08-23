package no.nav.k9punsj.journalpost.dto

data class KopierJournalpostDto(
    val fra: String,
    val til: String,
    val barn: String?,
    val annenPart: String?
) {
    init {
        require(barn != null || annenPart != null) {
            "Må sette minst en av barn og annenPart"
        }
    }
}