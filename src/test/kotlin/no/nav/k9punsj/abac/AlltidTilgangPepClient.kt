package no.nav.k9punsj.abac

import no.nav.k9punsj.LokalProfil
import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@LokalProfil
@Profile("test") // TODO Fjern denne når vi har fått ordnet opp i MockkBean for IPepClient.
internal class AlltidTilgangPepClient : IPepClient {
    override suspend fun harLesetilgang(fnr: List<String>, fnrForSporingslogg: List<String>, urlKallet: String) = true
    override suspend fun harLesetilgang(fnr: String, urlKallet: String) = true
    override suspend fun harSendeInnTilgang(fnr: String, urlKallet: String) = true
    override suspend fun harSendeInnTilgang(fnr: List<String>, fnrForSporingslogg: List<String>, urlKallet: String) = true
    override suspend fun erSaksbehandler() = true
}
