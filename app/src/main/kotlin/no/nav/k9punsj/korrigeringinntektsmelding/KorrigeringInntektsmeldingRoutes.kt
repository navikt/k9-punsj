package no.nav.k9punsj.korrigeringinntektsmelding

import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.norskIdent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType

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
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.norskIdent(), url = request.path())
                ?.let { return@GET it }
            korrigeringInntektsmeldingService.henteMappe(request)
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            korrigeringInntektsmeldingService.henteSøknad(request)
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            //TODO("Har ny søknad ID i header?")
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.norskIdent(), url = request.path())
                ?.let { return@POST it }
            korrigeringInntektsmeldingService.nySøknad(request)
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            korrigeringInntektsmeldingService.oppdaterEksisterendeSøknad(request)
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.norskIdent(), url = request.path())
                ?.let { return@POST it }
            korrigeringInntektsmeldingService.sendEksisterendeSøknad(request)
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.norskIdent(), url = request.path())
                ?.let { return@POST it }
            korrigeringInntektsmeldingService.validerSøknad(request)
        }

        POST("/api${Urls.HentArbeidsforholdIderFraK9sak}") { request ->
            innlogget.harInnloggetBrukerTilgangTil(norskIdentDto = listOf(request.norskIdent()), url = request.path())
                ?.let { return@POST it }
            korrigeringInntektsmeldingService.hentArbeidsforholdIderFraK9Sak(request)
        }
    }
}





