package no.nav.k9punsj.person

import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import no.nav.k9punsj.person.PersonRoutesConfiguration.Urls.HenteBarn
import no.nav.k9punsj.person.PersonRoutesConfiguration.Urls.HentePerson
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class PersonRoutesConfiguration(
    private val authenticationHandler: AuthenticationHandler,
    private val pepClient: IPepClient,
    private val barnService: BarnService,
    private val pdlService: PdlService
) {

    internal object Urls {
        internal const val HenteBarn = "/barn"
        internal const val HentePerson = "/person"
    }

    @Bean
    fun personRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${HentePerson}") { request ->
            RequestContext(coroutineContext, request) {
                val identitetsnummer = request.identitetsnummer()

                harInnloggetBrukerTilgangTil(identitetsnummer)?.let { return@RequestContext it }

                val person = pdlService.hentPersonopplysninger(setOf(identitetsnummer)).first().let { Person(
                    identitetsnummer = identitetsnummer,
                    fødselsdato = it.fødselsdato,
                    fornavn = it.fornavn,
                    mellomnavn = it.mellomnavn,
                    etternavn = it.etternavn
                )}

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(person)
            }
        }
        GET("/api${HenteBarn}") { request ->
            RequestContext(coroutineContext, request) {
                val identitetsnummer = request.identitetsnummer()

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

    private fun ServerRequest.identitetsnummer(): String {
        return requireNotNull(headers().header("X-Nav-NorskIdent").firstOrNull()) {
            "Mangler identitetsnummer"
        }.also { require(it.matches("\\d{11,20}".toRegex())) {
            "Ugyldig identitetsnummer"
        }}
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