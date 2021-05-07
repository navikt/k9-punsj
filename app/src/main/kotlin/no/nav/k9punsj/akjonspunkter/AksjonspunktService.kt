package no.nav.k9punsj.akjonspunkter

interface AksjonspunktService {


    suspend fun opprettAksjonspunktOgSendTilK9Los(journalpostId: String, aksjonspunkt : Pair<AksjonspunktKode, AksjonspunktStatus>)

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
