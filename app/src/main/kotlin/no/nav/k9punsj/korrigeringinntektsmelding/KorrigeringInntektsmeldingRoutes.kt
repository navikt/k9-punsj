package no.nav.k9punsj.korrigeringinntektsmelding

import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.Urls
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnRoutes
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.norskIdent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration
internal class KorrigeringInntektsmeldingRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
    private val korrigeringInntektsmeldingService: KorrigeringInntektsmeldingService
) {

    private companion object {
        private const val søknadType = "omsorgspenger-soknad"
    }

    @Bean
    fun omsorgspengerSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api/$søknadType/${Urls.HenteMappe}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdentDto = request.norskIdent(), url = request.path())
                ?.let { return@GET it }
            korrigeringInntektsmeldingService.henteMappe(request)
        }

        GET("/api/$søknadType/${Urls.HenteSøknad}") {
            korrigeringInntektsmeldingService.henteSøknad(it)
        }

        POST("/api/$søknadType/${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            //TODO("Har ny søknad ID i header?")
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdentDto = request.norskIdent(), url = request.path())
                ?.let { return@POST it }
            korrigeringInntektsmeldingService.nySøknad(request)
        }

        PUT("/api/$søknadType/${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) {
            korrigeringInntektsmeldingService.oppdaterEksisterendeSøknad(it)
        }

        POST("/api/$søknadType/${Urls.SendEksisterendeSøknad}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdentDto = request.norskIdent(), url = request.path())
                ?.let { return@POST it }
            korrigeringInntektsmeldingService.sendEksisterendeSøknad(request)
        }

        POST("/api/$søknadType/${Urls.ValiderSøknad}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdentDto = request.norskIdent(), url = request.path())
                ?.let { return@POST it }
            korrigeringInntektsmeldingService.validerSøknad(request)
        }

        POST("/api/$søknadType/${Urls.HentArbeidsforholdIderFraK9sak}") { request ->
            innlogget.harInnloggetBrukerTilgangTil(norskIdentDto = listOf(request.norskIdent()), url = request.path())
                ?.let { return@POST it }
            korrigeringInntektsmeldingService.hentArbeidsforholdIderFraK9Sak(request)
        }
    }
}





