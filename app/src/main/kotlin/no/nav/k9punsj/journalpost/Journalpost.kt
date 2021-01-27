package no.nav.k9punsj.journalpost

import no.nav.k9punsj.AktørId
import no.nav.k9punsj.JournalpostId
import java.util.*

data class Journalpost(
        val uuid: UUID,
        val journalpostId: JournalpostId,
        val aktørId: AktørId?
)
