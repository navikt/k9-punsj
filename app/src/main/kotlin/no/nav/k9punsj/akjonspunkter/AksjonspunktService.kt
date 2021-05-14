package no.nav.k9punsj.akjonspunkter

import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.VentDto

interface AksjonspunktService {


    suspend fun opprettAksjonspunktOgSendTilK9Los(journalpost: Journalpost, aksjonspunkt : Pair<AksjonspunktKode, AksjonspunktStatus>)

    suspend fun settUtførtAksjonspunktOgSendLukkOppgaveTilK9Los(
        journalpostId: String,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>
    )

    suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(
        journalpostId: String
    )

    suspend fun settPåVentOgSendTilLos(journalpostId: String)

    suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: List<String>)

    suspend fun settUtførtForAksjonspunkterOgSendLukkOppgaveTilK9Los(
        journalpostId: List<String>,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>
    )

    suspend fun sjekkOmDenErPåVent(journalpostId: String) : VentDto?
}
