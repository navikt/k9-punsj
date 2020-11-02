package no.nav.k9.fordel

import no.nav.k9.AktørId
import no.nav.k9.JournalpostId
import no.nav.k9.NorskIdent
import java.time.LocalDateTime

data class PunsjEventDto(
        val eksternId: String,
        val journalpostId: JournalpostId,
        val aktørId: AktørId?,
        val eventTid: LocalDateTime
)
