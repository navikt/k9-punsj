package no.nav.k9punsj.akjonspunkter

import no.nav.k9punsj.rest.web.JournalpostId
import java.time.LocalDateTime

typealias AksjonspunktId = String

data class AksjonspunktEntitet(
    val aksjonspunktId: AksjonspunktId,
    val aksjonspunktKode: AksjonspunktKode,
    val journalpostId: JournalpostId,
    val aksjonspunktStatus: AksjonspunktStatus,
    val frist_tid: LocalDateTime? = null,
    val vent_årsak: VentÅrsakType? = null,
    val opprettet_av: String? = null,
    val opprettet_tid: LocalDateTime? = null,
    val endret_av: String? = null,
    val endret_tid: LocalDateTime? = null,
)

