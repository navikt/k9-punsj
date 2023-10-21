package no.nav.k9punsj.omsorgspengerkronisksyktbarn

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import no.nav.k9punsj.utils.ServerRequestUtils.mapNySøknad
import no.nav.k9punsj.utils.ServerRequestUtils.mapSendSøknad
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import kotlin.coroutines.coroutineContext

@Configuration
internal class OmsorgspengerKroniskSyktBarnRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val innlogget: InnloggetUtils,
    private val omsorgspengerKroniskSyktBarnService: OmsorgspengerKroniskSyktBarnService
) {

    private companion object {
        const val søknadType = "omsorgspenger-kronisk-sykt-barn-soknad"
        const val SøknadIdKey = "soeknad_id"
    }

    internal object Urls {
        const val HenteMappe = "/$søknadType/mappe" // get
        const val HenteSøknad = "/$søknadType/mappe/{$SøknadIdKey}" // get
        const val NySøknad = "/$søknadType" // post
        const val OppdaterEksisterendeSøknad = "/$søknadType/oppdater" // put
        const val SendEksisterendeSøknad = "/$søknadType/send" // post
        const val ValiderSøknad = "/$søknadType/valider" // post
    }

    @Bean
    fun omsorgspengerKroniskSyktBarnSøknadRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HenteMappe}") { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentNorskIdentHeader()
                innlogget.harInnloggetBrukerTilgangTilOgSkriveSakForFnr(
                    fnr = norskIdent,
                    url = Urls.HenteMappe
                )?.let { return@RequestContext it }

                omsorgspengerKroniskSyktBarnService.henteMappe(norskIdent)
            }
        }

        GET("/api${Urls.HenteSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val søknadId = request.søknadId()
                omsorgspengerKroniskSyktBarnService.henteSøknad(søknadId)
            }
        }

        POST("/api${Urls.NySøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val nySøknad = request.mapNySøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSkriveSakForFnr(
                    fnr = nySøknad.norskIdent,
                    url = Urls.NySøknad
                )?.let { return@RequestContext it }

                omsorgspengerKroniskSyktBarnService.nySøknad(request, nySøknad)
            }
        }

        PUT("/api${Urls.OppdaterEksisterendeSøknad}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val søknad = request.omsorgspengerKroniskSyktBarnSøknad()
                omsorgspengerKroniskSyktBarnService.oppdaterEksisterendeSøknad(søknad)
            }
        }

        POST("/api${Urls.SendEksisterendeSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val sendSøknad = request.mapSendSøknad()
                innlogget.harInnloggetBrukerTilgangTilOgSkriveSakForFnr(
                    fnr = sendSøknad.norskIdent,
                    url = Urls.SendEksisterendeSøknad
                )?.let { return@RequestContext it }

                omsorgspengerKroniskSyktBarnService.sendEksisterendeSøknad(sendSøknad)
            }
        }

        POST("/api${Urls.ValiderSøknad}") { request ->
            RequestContext(coroutineContext, request) {
                val soknadTilValidering = request.omsorgspengerKroniskSyktBarnSøknad()
                soknadTilValidering.soekerId?.let { norskIdent ->
                    innlogget.harInnloggetBrukerTilgangTilOgSkriveSakForFnr(
                        fnr = norskIdent,
                        url = Urls.ValiderSøknad
                    )?.let { return@RequestContext it }
                }

                omsorgspengerKroniskSyktBarnService.validerSøknad(soknadTilValidering)
            }
        }
    }

    private fun ServerRequest.søknadId(): String = pathVariable(SøknadIdKey)

    private suspend fun ServerRequest.omsorgspengerKroniskSyktBarnSøknad() =
        body(BodyExtractors.toMono(OmsorgspengerKroniskSyktBarnSøknadDto::class.java)).awaitFirst()
}
