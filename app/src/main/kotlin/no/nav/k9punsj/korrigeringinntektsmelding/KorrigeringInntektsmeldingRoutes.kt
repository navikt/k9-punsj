package no.nav.k9punsj.korrigeringinntektsmelding

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
internal class KorrigeringInntektsmeldingRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
    private val korrigeringInntektsmeldingService: KorrigeringInntektsmeldingService
) {

    private companion object {
        private const val søknadType = "omsorgspenger-soknad"
        private const val SøknadIdKey = "soeknad_id"
    }

    internal object Urls {
        const val HenteMappe = "/$søknadType/mappe" //get
        const val HenteSøknad = "/$søknadType/mappe/{$SøknadIdKey}" //get
        const val NySøknad = "/$søknadType" //post
        const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" //put
        const val SendEksisterendeSøknad = "/$søknadType/send" //post
        const val ValiderSøknad = "/$søknadType/valider" //post
        const val HentArbeidsforholdIderFraK9sak = "/$søknadType/k9sak/arbeidsforholdIder" //post
    }

    @Bean
    fun omsorgspengerSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = norskIdent,
                    url = request.path()
                )?.let { return@RequestContext it }

                return@RequestContext korrigeringInntektsmeldingService.henteMappe(norskIdent)
            }
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknadId = request.søknadId()
                return@RequestContext korrigeringInntektsmeldingService.henteSøknad(søknadId)
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = request.hentNorskIdentHeader(),
                    url = request.path()
                )?.let { return@RequestContext it }

                val opprettNySøknad = request.mapNySøknad()
                return@RequestContext korrigeringInntektsmeldingService.nySøknad(request, opprettNySøknad)
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.korrigeringInntektsmelding()
                return@RequestContext korrigeringInntektsmeldingService.oppdaterEksisterendeSøknad(søknad)
            }
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = request.hentNorskIdentHeader(),
                    url = request.path()
                )?.let { return@RequestContext it }

                val sendSøknad = request.mapSendSøknad()
                return@RequestContext korrigeringInntektsmeldingService.sendEksisterendeSøknad(sendSøknad)
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                innlogget.harInnloggetBrukerTilgangTilOgSendeInn(
                    norskIdent = request.hentNorskIdentHeader(),
                    url = request.path()
                )?.let { return@RequestContext it }

                val søknad = request.korrigeringInntektsmelding()
                return@RequestContext korrigeringInntektsmeldingService.validerSøknad(søknad)
            }
        }

        POST("/api${Urls.HentArbeidsforholdIderFraK9sak}") { request ->
            RequestContext(coroutineContext, request) {
                innlogget.harInnloggetBrukerTilgangTil(
                    norskIdentDto = listOf(request.hentNorskIdentHeader()),
                    url = request.path()
                )?.let { return@RequestContext it }
                val matchMedPeriode = request.mapMatchFagsakMedPerioder()
                return@RequestContext korrigeringInntektsmeldingService.hentArbeidsforholdIderFraK9Sak(matchMedPeriode)
            }
        }
    }

    private fun ServerRequest.søknadId(): String = pathVariable(SøknadIdKey)

    private suspend fun ServerRequest.korrigeringInntektsmelding() =
        body(BodyExtractors.toMono(KorrigeringInntektsmeldingDto::class.java)).awaitFirst()
}





