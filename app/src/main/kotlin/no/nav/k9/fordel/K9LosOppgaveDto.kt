package no.nav.k9.fordel

import no.nav.k9.JournalpostId
import java.time.LocalDateTime

data class PunsjEventDto(
        val eksternId: String,
        val journalpostId: JournalpostId,
        val eventTid: LocalDateTime
)