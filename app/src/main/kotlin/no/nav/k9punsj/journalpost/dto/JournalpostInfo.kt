package no.nav.k9punsj.journalpost.dto

import java.time.LocalDateTime

data class JournalpostInfo(
    val journalpostId: String,
    val norskIdent: String?,
    val aktørId: String?,
    val dokumenter: List<DokumentInfo>,
    val mottattDato: LocalDateTime,
    val erInngående: Boolean,
    val kanOpprettesJournalføringsoppgave: Boolean,
    val journalpostStatus: String,
)
