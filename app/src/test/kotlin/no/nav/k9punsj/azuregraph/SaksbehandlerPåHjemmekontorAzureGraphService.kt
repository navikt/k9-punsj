package no.nav.k9punsj.azuregraph

import org.springframework.stereotype.Component

@Component
internal class SaksbehandlerPåHjemmekontorAzureGraphService : IAzureGraphService {
    override suspend fun hentIdentTilInnloggetBruker() = "saksbehandler@nav.no"
    override suspend fun hentEnhetForInnloggetBruker() = "Hjemmekontor"
}