package no.nav.k9.journalpost

import no.nav.k9.AktørId
import no.nav.k9.JournalpostId
import java.util.*

data class Journalpost(
        val uuid: UUID,
        val journalpostId: JournalpostId,
        val aktørId: AktørId?
)
