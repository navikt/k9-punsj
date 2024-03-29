package no.nav.k9punsj.tilgangskontroll

import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json

@Configuration
class InnloggetUtils(
    private val pepClient: IPepClient
) {

    internal suspend fun harInnloggetBrukerTilgangTilOgSendeInn(
        norskIdent: String,
        url: String
    ): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.sendeInnTilgang(norskIdent, url)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til og sende på denne personen")
        }
        return null
    }

    internal suspend fun harInnloggetBrukerTilgangTil(
        norskIdentDto: List<String>,
        url: String
    ): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.sendeInnTilgang(norskIdentDto, url)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til å slå opp denne personen")
        }
        return null
    }
}
