package no.nav.k9punsj.journalpost

import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.rest.web.JournalpostId
import java.time.LocalDateTime
import java.util.UUID

data class Journalpost(
        val uuid: UUID,
        val journalpostId: JournalpostId,
        val aktørId: AktørId?,
        val skalTilK9 : Boolean? = null,
        val mottattDato : LocalDateTime? = null,
        val type : String? = null,
        val payload : String? = null
)
