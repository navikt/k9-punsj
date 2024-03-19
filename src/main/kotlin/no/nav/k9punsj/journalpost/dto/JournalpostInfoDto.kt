package no.nav.k9punsj.journalpost.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.fordel.K9FordelType

data class JournalpostInfoDto(
    val journalpostId: String,
    val norskIdent: String?,
    val dokumenter: List<DokumentInfo>,
    val venter: VentDto?,
    val punsjInnsendingType: K9FordelType?, // Kan ikke bytte navn på denne, brukes av frontend?
    @JsonIgnore
    val erInngående: Boolean,
    val kanSendeInn: Boolean,
    val erSaksbehandler: Boolean? = null,
    val journalpostStatus: String,
    val kanOpprettesJournalføringsoppgave: Boolean, // Brukes av frontend
    val kanKopieres: Boolean = punsjInnsendingType != K9FordelType.KOPI && erInngående, // Brukes av frontend,
    val erFerdigstilt: Boolean, // Brukes av frontend for å bestemme om ytelse å fagsak må settes før punsjing. (Ref: Postmottak)
    val gosysoppgaveId: String?,
    val sak: Sak?
)
