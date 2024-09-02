package no.nav.k9punsj.akjonspunkter

import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.journalpost.dto.VentDto

interface AksjonspunktService {

    suspend fun opprettAksjonspunktOgSendTilK9Los(
        punsjJournalpost: PunsjJournalpost,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
        type: String?,
        ytelse: String?,
        pleietrengendeAktørId: String?
    )

    suspend fun settPåVentOgSendTilLos(journalpostId: String, søknadId: String?)

    suspend fun sjekkOmDenErPåVent(journalpostId: String): VentDto?

    suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: Collection<String>, erSendtInn: Boolean, ansvarligSaksbehandler: String?)
    suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: String, erSendtInn: Boolean, ansvarligSaksbehandler: String?)
}
