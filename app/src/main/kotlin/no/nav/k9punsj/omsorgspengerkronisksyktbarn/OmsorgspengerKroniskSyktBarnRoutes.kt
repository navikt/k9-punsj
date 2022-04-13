package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.Urls
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.norskIdent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType

@Configuration
internal class OmsorgspengerKroniskSyktBarnRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
    private val omsorgspengerKroniskSyktBarnService: OmsorgspengerKroniskSyktBarnService
) {

    internal val søknadType = "omsorgspenger-kronisk-sykt-barn-soknad"

    @Bean
    fun omsorgspengerKroniskSyktBarnSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api/$søknadType/${Urls.HenteMappe}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.norskIdent(), url = Urls.HenteMappe)
                ?.let { return@GET it }
            omsorgspengerKroniskSyktBarnService.henteMappe(request)
        }

        GET("/api/$søknadType/${Urls.HenteSøknad}") {
            omsorgspengerKroniskSyktBarnService.henteSøknad(it)
        }

        POST("/api/$søknadType/${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.norskIdent(), url = Urls.NySøknad)
                ?.let { return@POST it }
            omsorgspengerKroniskSyktBarnService.nySøknad(request)
        }

        PUT("/api/$søknadType/${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) {
            omsorgspengerKroniskSyktBarnService.oppdaterEksisterendeSøknad(it)
        }

        POST("/api/$søknadType/${Urls.SendEksisterendeSøknad}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.norskIdent(), url = Urls.SendEksisterendeSøknad)
                ?.let { return@POST it }
            omsorgspengerKroniskSyktBarnService.sendEksisterendeSøknad(request)
        }

        POST("/api/$søknadType/${Urls.ValiderSøknad}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.norskIdent(), url = Urls.ValiderSøknad)
                ?.let { return@POST it }
            omsorgspengerKroniskSyktBarnService.validerSøknad(request)
        }
    }

}





