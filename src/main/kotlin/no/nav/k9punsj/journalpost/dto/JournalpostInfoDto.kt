package no.nav.k9punsj.journalpost.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.k9punsj.fordel.PunsjInnsendingType

data class JournalpostInfoDto(
    val journalpostId: String,
    val norskIdent: String?,
    val dokumenter: List<DokumentInfo>,
    val venter: VentDto?,
    val punsjInnsendingType: PunsjInnsendingType?,
    @JsonIgnore
    val erInngående: Boolean,
    val kanSendeInn: Boolean,
    val erSaksbehandler: Boolean? = null,
    val journalpostStatus: String,
    val kanOpprettesJournalføringsoppgave: Boolean, // Brukes av frontend
    val kanKopieres: Boolean = punsjInnsendingType != PunsjInnsendingType.KOPI && erInngående, // Brukes av frontend,
    val gosysoppgaveId: String?,
)
