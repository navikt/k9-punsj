package no.nav.k9.fordel

import no.nav.k9.JournalpostId
import java.time.LocalDateTime
import java.util.*

data class PunsjEventDto(
        val eksternId: UUID,
        val journalpostId: JournalpostId,
        val eventTid: LocalDateTime
)