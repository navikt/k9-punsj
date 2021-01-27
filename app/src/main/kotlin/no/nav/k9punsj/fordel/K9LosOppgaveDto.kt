package no.nav.k9punsj.fordel

import no.nav.k9punsj.AktørId
import no.nav.k9punsj.JournalpostId
import java.time.LocalDateTime

data class PunsjEventDto(
        val eksternId: String,
        val journalpostId: JournalpostId,
        val aktørId: AktørId?,
        val eventTid: LocalDateTime,
        val aksjonspunktKoderMedStatusListe: MutableMap<String, String>
)
