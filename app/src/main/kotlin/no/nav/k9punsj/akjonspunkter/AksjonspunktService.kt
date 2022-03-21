package no.nav.k9punsj.akjonspunkter

import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.journalpost.VentDto
import no.nav.k9punsj.domenetjenester.dto.SøknadIdDto

interface AksjonspunktService {


    suspend fun opprettAksjonspunktOgSendTilK9Los(
        punsjJournalpost: PunsjJournalpost,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
        type: String?,
        ytelse: String?
    )

    suspend fun settUtførtAksjonspunktOgSendLukkOppgaveTilK9Los(
        journalpostId: String,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>
    )

    suspend fun settPåVentOgSendTilLos(journalpostId: String, søknadId: SøknadIdDto?)

    suspend fun settUtførtForAksjonspunkterOgSendLukkOppgaveTilK9Los(
        journalpostId: List<String>,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>
    )

    suspend fun sjekkOmDenErPåVent(journalpostId: String) : VentDto?
    suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: List<String>, erSendtInn: Boolean)
    suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: String, erSendtInn: Boolean)
}
