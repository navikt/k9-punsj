package no.nav.k9punsj.abac

import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import org.springframework.stereotype.Component

@Component
internal class AlltidTilgangPepClient : IPepClient {
    override suspend fun harBasisTilgang(fnr: List<String>, urlKallet: String) = true
    override suspend fun harBasisTilgang(fnr: String, urlKallet: String) = true
    override suspend fun sendeInnTilgang(fnr: String, urlKallet: String) = true
    override suspend fun sendeInnTilgang(fnr: List<String>, urlKallet: String) = true
    override suspend fun erSaksbehandler() = true
}