package no.nav.k9punsj.journalpost.dto

import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto

data class JournalpostIderDto(
    val journalpostIder: List<JournalpostIdDto>,
    val journalpostIderBarn: List<JournalpostIdDto> = emptyList()
)
