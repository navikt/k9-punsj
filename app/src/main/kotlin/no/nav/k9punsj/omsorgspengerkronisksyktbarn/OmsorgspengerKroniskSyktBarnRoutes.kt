package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType

@Configuration
internal class OmsorgspengerKroniskSyktBarnRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
    private val omsorgspengerKroniskSyktBarnService: OmsorgspengerKroniskSyktBarnService
) {

    private companion object {
        const val søknadType = "omsorgspenger-kronisk-sykt-barn-soknad"
    }


    internal object Urls {
        const val HenteMappe = "/$søknadType/mappe" //get
        const val HenteSøknad = "/$søknadType/mappe/soeknad_id" //get
        const val NySøknad = "/$søknadType" //post
        const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" //put
        const val SendEksisterendeSøknad = "/$søknadType/send" //post
        const val ValiderSøknad = "/$søknadType/valider" //post
    }

    @Bean
    fun omsorgspengerKroniskSyktBarnSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.hentNorskIdentHeader(), url = Urls.HenteMappe)
                ?.let { return@GET it }
            omsorgspengerKroniskSyktBarnService.henteMappe(request)
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            omsorgspengerKroniskSyktBarnService.henteSøknad(request)
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.hentNorskIdentHeader(), url = Urls.NySøknad)
                ?.let { return@POST it }
            omsorgspengerKroniskSyktBarnService.nySøknad(request)
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            omsorgspengerKroniskSyktBarnService.oppdaterEksisterendeSøknad(request)
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.hentNorskIdentHeader(), url = Urls.SendEksisterendeSøknad)
                ?.let { return@POST it }
            omsorgspengerKroniskSyktBarnService.sendEksisterendeSøknad(request)
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            innlogget.harInnloggetBrukerTilgangTilOgSendeInn(norskIdent = request.hentNorskIdentHeader(), url = Urls.ValiderSøknad)
                ?.let { return@POST it }
            omsorgspengerKroniskSyktBarnService.validerSøknad(request)
        }
    }

}





