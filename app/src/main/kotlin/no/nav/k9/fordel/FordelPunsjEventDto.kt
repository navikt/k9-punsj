package no.nav.k9.fordel

import no.nav.k9.JournalpostId

data class FordelPunsjEventDto(
        val aktørId: no.nav.k9.AktørId? = null,
        val journalpostId: JournalpostId
)