package no.nav.k9punsj.akjonspunkter

import no.nav.k9punsj.journalpost.Journalpost

interface AksjonspunktService {


    suspend fun opprettAksjonspunktOgSendTilK9Los(journalpost: Journalpost, aksjonspunkt : Pair<AksjonspunktKode, AksjonspunktStatus>)

    suspend fun settUtførtAksjonspunktOgSendLukkOppgaveTilK9Los(
        journalpostId: String,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>
    )

    suspend fun settPåVent(journalpostId: String)


    suspend fun settUtførtForAksjonspunkterOgSendLukkOppgaveTilK9Los(
        journalpostId: List<String>,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>
    )
}
