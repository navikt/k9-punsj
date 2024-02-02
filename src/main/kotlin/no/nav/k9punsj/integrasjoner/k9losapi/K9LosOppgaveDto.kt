package no.nav.k9punsj.integrasjoner.k9losapi

import java.time.LocalDateTime

data class PunsjEventDto(
    val eksternId: String, // Unik id i journalpostId
    val journalpostId: String,
    val aktørId: String?,
    val eventTid: LocalDateTime, // Brukes av LOS for å differensiere versjon, mappes til ekstern_versjon
    val aksjonspunktKoderMedStatusListe: MutableMap<String, String>, // Slettes når los er over på ny modell og bruker K9LosOppgaveStatusDto
    val pleietrengendeAktørId: String? = null, // Slettes om vi ikke trenger funksjonalitet på og reservere oppgaver på tvers av pleietrengende.
    val type: String, // Skall ikke vara nullable, null = ukjent
    val ytelse: String? = null,
    val sendtInn: Boolean? = null, // Slettes, erstattes med status UTFØRT
    val ferdigstiltAv: String? = null, // Slettes
    val mottattDato: LocalDateTime? = null,
    val status: K9LosOppgaveStatusDto? = null
)