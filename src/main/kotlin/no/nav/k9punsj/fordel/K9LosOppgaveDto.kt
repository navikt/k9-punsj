package no.nav.k9punsj.fordel

import java.time.LocalDateTime

data class PunsjEventDto(
    val eksternId: String,
    val journalpostId: String,
    val aktørId: String?,
    val eventTid: LocalDateTime,
    val aksjonspunktKoderMedStatusListe: MutableMap<String, String>,
    val pleietrengendeAktørId: String? = null,
    val type: String? = null,
    val ytelse: String? = null,
    val sendtInn: Boolean? = null,
    val ferdigstiltAv: String? = null
)
