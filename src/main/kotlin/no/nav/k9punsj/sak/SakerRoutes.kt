package no.nav.k9punsj.sak

import kotlinx.coroutines.currentCoroutineContext
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.tilgangskontroll.InnloggetUtils
import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import no.nav.k9punsj.utils.ServerRequestUtils.hentNorskIdentHeader
import no.nav.sif.abac.kontrakt.abac.dto.SaksnummerDto
import no.nav.sif.abac.kontrakt.person.AktørId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json

@Configuration
internal class SakerRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val sakService: SakService,
    private val innloggetUtils: InnloggetUtils,
    private val pepClient: IPepClient
) {

    internal companion object {
        private val logger: Logger = LoggerFactory.getLogger(SakerRoutes::class.java)
    }

    internal object Urls {
        internal const val HentSaker = "/saker/hent"
        internal const val HentPerioder = "/saker/perioder"
    }

    @Bean
    fun SakerRoutes(personService: PersonService) = SaksbehandlerRoutes(authenticationHandler) {
        GET("/api${Urls.HentSaker}") { request ->
            //TODO foretrekk POST og send fnr i body. GET med fnr i header er ikke ideelt.
            RequestContext(currentCoroutineContext(), request) {
                val norskIdent = request.hentNorskIdentHeader()
                val aktørId = personService.finnAktørId(norskIdent)
                val tilgang = pepClient.sjekkTilgangTilBrukersSakerOgGiInformasjonOmHistoriskSak(
                        brukerAktørId = AktørId(aktørId),
                        urlKallet = Urls.HentSaker
                    )
                if (!tilgang.tilgangsbeslutning.harTilgang){
                    return@RequestContext ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .json()
                        .bodyValueAndAwait(tilgang.tilgangsbeslutning.årsakerForIkkeTilgang)
                }

                val saker = try {
                    sakService.hentSaker(norskIdent)
                } catch (e: Exception) {
                    logger.error("Feilet med å hente saker.", e)
                    return@RequestContext ServerResponse
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json()
                        .bodyValueAndAwait(OasFeil(e.message))
                }

                //kallet til sakService returnerer også reserverte saker, de er ikke tilgangssjekket allerede så gjør det her
                val reserverteSaksnumre = saker.filter { s->s.reservert }.map { s->Saksnummer(s.fagsakId) }
                reserverteSaksnumre.forEach {
                    val tilgangsbeslutning  = pepClient.harLesetilgangTilSaksnummer(it, Urls.HentSaker)
                    if (!tilgangsbeslutning.harTilgang) {
                        return@RequestContext ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .json()
                            .bodyValueAndAwait(tilgangsbeslutning.årsakerForIkkeTilgang)
                    }
                }

                return@RequestContext ServerResponse
                    .status(HttpStatus.OK)
                    .json()
                    .bodyValueAndAwait(saker)
            }
        }

        POST("/api${Urls.HentPerioder}") { request ->
            RequestContext(currentCoroutineContext(), request) {
                val saksnummer = request.queryParam("saksnummer").orElseThrow()

                val tilgangsbeslutning  = pepClient.harLesetilgangTilSaksnummer(Saksnummer(saksnummer), Urls.HentSaker)
                if (!tilgangsbeslutning.harTilgang) {
                    return@RequestContext ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .json()
                        .bodyValueAndAwait(tilgangsbeslutning.årsakerForIkkeTilgang)
                }

                val perioder = try {
                    sakService.hentPerioderForSaksnummer(saksnummer)
                } catch (e: Exception) {
                    logger.error("Feilet med å hente saker.", e)
                    return@RequestContext ServerResponse
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json()
                        .bodyValueAndAwait(OasFeil(e.message))
                }

                ServerResponse
                    .ok()
                    .json()
                    .bodyValueAndAwait(perioder)
            }
        }
    }
}
