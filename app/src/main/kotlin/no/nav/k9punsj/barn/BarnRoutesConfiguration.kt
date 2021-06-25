package no.nav.k9punsj.barn

import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.abac.IPepClient
import no.nav.k9punsj.barn.BarnRoutesConfiguration.Urls.HenteBarn
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class BarnRoutesConfiguration(
    private val authenticationHandler: AuthenticationHandler,
    private val pepClient: IPepClient,
    private val barnService: BarnService) {

    internal object Urls {
        internal const val HenteBarn = "/barn"
    }

    @Bean
    fun barnRoutes() = Routes(authenticationHandler) {
        GET("/api${HenteBarn}") { request ->
            RequestContext(coroutineContext, request) {
                val identitetsnummer = request.norskeIdent()

                harInnloggetBrukerTilgangTil(identitetsnummer)?.let { return@RequestContext it }

                // I forbindelse med uthenting av barn filtrerer vi bort alle barn som har en form for gradering.
                // Gjøres derfor ingen sjekk på tilgang mot barna her.
                val barn = barnService.hentBarn(identitetsnummer)

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(BarnResponse(barn))

            }
        }
    }

    private fun ServerRequest.norskeIdent(): String {
        return headers().header("X-Nav-NorskIdent").first()!!
    }

    private suspend fun harInnloggetBrukerTilgangTil(identitetsnummer: String): ServerResponse? {
        if (!pepClient.harBasisTilgang(identitetsnummer, HenteBarn)) {
            return ServerResponse
                .status(HttpStatus.FORBIDDEN)
                .json()
                .bodyValueAndAwait("Du har ikke lov til å slå opp denne personen")
        }
        return null
    }
}