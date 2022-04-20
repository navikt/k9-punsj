package no.nav.k9punsj.pleiepengerlivetssluttfase

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import no.nav.k9punsj.utils.ServerRequestUtils.mapMatchFagsak
import no.nav.k9punsj.utils.ServerRequestUtils.mapNySøknad
import no.nav.k9punsj.utils.ServerRequestUtils.mapSendSøknad
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import kotlin.coroutines.coroutineContext

@Configuration
internal class PleiepengerLivetsSluttfaseRoutes(
    private val pleiepengerLivetsSluttfaseService: PleiepengerLivetsSluttfaseService,
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PleiepengerLivetsSluttfaseRoutes::class.java)
        private const val søknadType = "pleiepenger-livets-sluttfase-soknad"
        private const val SøknadIdKey = "soeknad_id"
    }

    internal object Urls {
        internal const val HenteMappe = "/$søknadType/mappe" //get
        internal const val HenteSøknad = "/$søknadType/mappe/{$SøknadIdKey}" //get
        internal const val NySøknad = "/$søknadType" //post
        internal const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" //put
        internal const val SendEksisterendeSøknad = "/$søknadType/send" //post
        internal const val ValiderSøknad = "/$søknadType/valider" //post
        internal const val HentInfoFraK9sak = "/$søknadType/k9sak/info" //post
    }

    @Bean
    fun pleiepengerLivetsSluttfaseSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = norskIdent, url = Urls.HenteMappe)
                    ?.let { return@RequestContext it }

                return@RequestContext pleiepengerLivetsSluttfaseService.henteMappe(norskIdent)
            }
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknadId = request.søknadId()
                return@RequestContext pleiepengerLivetsSluttfaseService.henteSøknad(søknadId)
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.pleiepengerPlsSøknad()
                return@RequestContext pleiepengerLivetsSluttfaseService.oppdaterEksisterendeSøknad(søknad)
            }
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val sendSøknad = request.mapSendSøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = sendSøknad.norskIdent,
                    url = Urls.SendEksisterendeSøknad
                )?.let { return@RequestContext it }

                return@RequestContext pleiepengerLivetsSluttfaseService.sendEksisterendeSøknad(sendSøknad)
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val opprettNySøknad = request.mapNySøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = opprettNySøknad.norskIdent,
                    url = Urls.NySøknad
                )?.let { return@RequestContext it }

                return@RequestContext pleiepengerLivetsSluttfaseService.nySøknad(request, opprettNySøknad)
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val soknadTilValidering = request.pleiepengerPlsSøknad()
                soknadTilValidering.soekerId?.let { norskIdent ->
                    innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                        norskIdent = norskIdent,
                        url = Urls.ValiderSøknad
                    )?.let { return@RequestContext it }
                }

                return@RequestContext pleiepengerLivetsSluttfaseService.validerSøknad(soknadTilValidering)
            }
        }

        POST("/api${Urls.HentInfoFraK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val matchfagsak = request.mapMatchFagsak()
                innlogget.harInnloggetBrukerTilgangTil(
                    norskIdentDto = listOf(matchfagsak.brukerIdent, matchfagsak.barnIdent),
                    url = Urls.HentInfoFraK9sak
                )?.let { return@RequestContext it }

                return@RequestContext pleiepengerLivetsSluttfaseService.hentInfoFraK9Sak(matchfagsak)
            }
        }
    }


    private fun ServerRequest.søknadId(): String = pathVariable(SøknadIdKey)

    private suspend fun ServerRequest.pleiepengerPlsSøknad() =
        body(BodyExtractors.toMono(PleiepengerLivetsSluttfaseSøknadDto::class.java)).awaitFirst()
}
