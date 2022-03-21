package no.nav.k9punsj.akjonspunkter

import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.VentDto
import no.nav.k9punsj.rest.web.dto.SøknadIdDto

interface AksjonspunktService {

    suspend fun opprettAksjonspunktOgSendTilK9Los(
        journalpost: Journalpost,
        aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>,
        type: String?,
        ytelse: String?
    )

    suspend fun settPåVentOgSendTilLos(journalpostId: String, søknadId: SøknadIdDto?)

    suspend fun sjekkOmDenErPåVent(journalpostId: String) : VentDto?

    suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: Collection<String>, erSendtInn: Boolean, ansvarligSaksbehandler: String?)
    suspend fun settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId: String, erSendtInn: Boolean, ansvarligSaksbehandler: String?)
    suspend fun settUtførtAksjonspunktOgSendLukkOppgaveTilK9Los(journalpostId: String, aksjonspunkt: Pair<AksjonspunktKode, AksjonspunktStatus>, ansvarligSaksbehandler: String?)
}
