package no.nav.k9punsj.person

import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import kotlin.coroutines.coroutineContext

@Configuration
internal class PersonRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val barnService: BarnService,
    private val pdlService: PdlService,
    private val personService: PersonService,
    private val innlogget: InnloggetUtils
) {

    internal object Urls {
        internal const val HenteBarn = "/barn"
        internal const val HentePerson = "/person"
        internal const val HenteAktørId = "/aktorId"
    }

    @Bean
    fun personRoute() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HentePerson}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilÅSendeInn(
                    fnr = norskIdent,
                    url = Urls.HentePerson
                )?.let { return@RequestContext it }

                val person = pdlService.hentPersonopplysninger(setOf(norskIdent)).first().let {
                    Person(
                        identitetsnummer = norskIdent,
                        fødselsdato = it.fødselsdato,
                        fornavn = it.fornavn,
                        mellomnavn = it.mellomnavn,
                        etternavn = it.etternavn
                    )
                }

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(person)
            }
        }
        GET("/api${Urls.HenteBarn}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilÅSendeInn(
                    fnr = norskIdent,
                    url = Urls.HenteBarn
                )?.let { return@RequestContext it }

                // I forbindelse med uthenting av barn filtrerer vi bort alle barn som har en form for gradering.
                // Gjøres derfor ingen sjekk på tilgang mot barna her.
                val barn = barnService.hentBarn(norskIdent)

                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(BarnResponse(barn))
            }
        }

        GET("/api${Urls.HenteAktørId}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilÅSendeInn(
                    fnr = norskIdent,
                    url = Urls.HenteAktørId
                )?.let { return@RequestContext it }

                val person = kotlin.runCatching { personService.finnPersonVedNorskIdentFørstDbSåPdl(norskIdent) }
                    .getOrElse {
                        return@RequestContext ServerResponse
                            .badRequest()
                            .json()
                            .bodyValueAndAwait(OasFeil(it.message!!))
                    }
                return@RequestContext ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(person.aktørId)
            }
        }
    }
}
