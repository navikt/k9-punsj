package no.nav.k9punsj.fordel

import no.nav.k9punsj.JournalpostId

data class FordelPunsjEventDto(
        val aktørId: no.nav.k9punsj.AktørId? = null,
        val journalpostId: JournalpostId
)
