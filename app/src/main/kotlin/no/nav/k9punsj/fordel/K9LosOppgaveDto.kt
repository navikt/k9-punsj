package no.nav.k9punsj.fordel

import no.nav.k9punsj.domenetjenester.dto.AktørIdDto
import no.nav.k9punsj.domenetjenester.dto.JournalpostIdDto
import java.time.LocalDateTime

data class PunsjEventDto(
    val eksternId: String,
    val journalpostId: JournalpostIdDto,
    val aktørId: AktørIdDto?,
    val eventTid: LocalDateTime,
    val aksjonspunktKoderMedStatusListe: MutableMap<String, String>,
    val pleietrengendeAktørId: String? = null,
    val type: String? = null,
    val ytelse: String? = null,
    val sendtInn: Boolean? = null,
    val ferdigstiltAv: String? = null
)
