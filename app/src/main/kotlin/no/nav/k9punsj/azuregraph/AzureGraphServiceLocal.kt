package no.nav.k9punsj.azuregraph

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local")
class AzureGraphServiceLocal : IAzureGraphService {
    override suspend fun hentIdentTilInnloggetBruker(): String {
        return "saksbehandler@nav.no"
    }

    override suspend fun hentEnhetForInnloggetBruker(): String {
        return "Hjemmekontor"
    }
}
