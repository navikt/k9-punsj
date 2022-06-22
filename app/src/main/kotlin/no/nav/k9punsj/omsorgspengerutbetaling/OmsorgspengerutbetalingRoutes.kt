package no.nav.k9punsj.omsorgspengerutbetaling

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import no.nav.k9punsj.utils.ServerRequestUtils.mapMatchFagsakMedPerioder
import no.nav.k9punsj.utils.ServerRequestUtils.mapNySøknad
import no.nav.k9punsj.utils.ServerRequestUtils.mapSendSøknad
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import kotlin.coroutines.coroutineContext

@Configuration
internal class OmsorgspengerutbetalingRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
    private val omsorgspengerutbetalingService: OmsorgspengerutbetalingService
) {

    private companion object {
        private const val søknadType = "omsorgspengerutbetaling-soknad"
        private const val SøknadIdKey = "soeknad_id"
    }

    internal object Urls {
        const val HenteMappe = "/$søknadType/mappe" // get
        const val HenteSøknad = "/$søknadType/mappe/{$SøknadIdKey}" // get
        const val NySøknad = "/$søknadType" // post
        const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" // put
        const val SendEksisterendeSøknad = "/$søknadType/send" // post
        const val ValiderSøknad = "/$søknadType/valider" // post
        const val HentArbeidsforholdIderFraK9sak = "/$søknadType/k9sak/arbeidsforholdIder" // post
    }

    @Bean
    fun omsorgspengerutbetalingSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = norskIdent,
                    url = request.path()
                )?.let { return@RequestContext it }

                omsorgspengerutbetalingService.henteMappe(norskIdent)
            }
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknadId = request.søknadId()
                omsorgspengerutbetalingService.henteSøknad(søknadId)
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val opprettNySøknad = request.mapNySøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = opprettNySøknad.norskIdent,
                    url = request.path()
                )?.let { return@RequestContext it }

                omsorgspengerutbetalingService.nySøknad(request, opprettNySøknad)
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.omsorgspengerutbetalingSøknadDto()
                omsorgspengerutbetalingService.oppdaterEksisterendeSøknad(søknad)
            }
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val sendSøknad = request.mapSendSøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = sendSøknad.norskIdent,
                    url = request.path()
                )?.let { return@RequestContext it }

                omsorgspengerutbetalingService.sendEksisterendeSøknad(sendSøknad)
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.omsorgspengerutbetalingSøknadDto()
                søknad.soekerId?.let { norskIdent ->
                    innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                        norskIdent = norskIdent,
                        url = Urls.ValiderSøknad
                    )?.let { return@let it }
                }

                omsorgspengerutbetalingService.validerSøknad(søknad)
            }
        }

        POST("/api${Urls.HentArbeidsforholdIderFraK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                val matchfagsakMedPeriode = request.mapMatchFagsakMedPerioder()
                innlogget.harInnloggetBrukerTilgangTil(
                    norskIdentDto = listOf(matchfagsakMedPeriode.brukerIdent),
                    url = Urls.HentArbeidsforholdIderFraK9sak
                )?.let { return@RequestContext it }

                omsorgspengerutbetalingService.hentArbeidsforholdIderFraK9Sak(matchfagsakMedPeriode)
            }
        }
    }

    private fun ServerRequest.søknadId(): String = pathVariable(SøknadIdKey)

    private suspend fun ServerRequest.omsorgspengerutbetalingSøknadDto() =
        body(BodyExtractors.toMono(OmsorgspengerutbetalingSøknadDto::class.java)).awaitFirst()
}
