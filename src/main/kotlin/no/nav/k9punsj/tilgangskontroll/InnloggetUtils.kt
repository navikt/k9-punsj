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

    internal suspend fun harInnloggetBrukerTilgangTilOgSkriveSakForFnr(
        fnr: String,
        url: String
    ): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.harInnloggetBrukerTilgangTilOgSkriveSakForFnr(fnr, url)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til og sende på denne personen")
        }
        return null
    }

    internal suspend fun harInnloggetBrukerTilgangTilOgSkriveSakForFnr(
        fnrList: List<String>,
        url: String
    ): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.harInnloggetBrukerTilgangTilOgSkriveSakForFnr(fnrList, url)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til å slå opp denne personen")
        }
        return null
    }

    internal suspend fun harInnloggetBrukerTilgangTilOgLeseSakForFnr(
        fnrList: List<String>,
        url: String
    ): ServerResponse? {
        val saksbehandlerHarTilgang = pepClient.harInnloggetBrukerTilgangTilOgLeseSakForFnr(fnrList, url)
        if (!saksbehandlerHarTilgang) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til å slå opp denne personen")
        }
        return null
    }

    @Deprecated("TODO: Sjekker tilgang til sak mot abac, ikke om bruker er saksbehandler?")
    internal suspend fun erInloggetBrukerSaksbehandlerIK9(): Boolean {
        // TODO: Skall sjekkes om inlogget bruker er i en av K9s AD saksbehandler grupper?
        return pepClient.erSaksbehandler()
    }
}
