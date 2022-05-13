package no.nav.k9punsj.integrasjoner.pdl

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.felles.IkkeTilgang
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import kotlin.coroutines.coroutineContext


@Configuration
internal class PdlRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val pdlService: PdlService
) {

    internal object Urls {
        internal const val HentIdent = "/pdl/hentident/"
    }

    @Bean
    fun PdlRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        POST("/api${Urls.HentIdent}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.hentSøknad().norskIdent
                try {
                    val pdlResponse = pdlService.identifikator(
                            fnummer = norskIdent
                    )
                    if (pdlResponse == null) {
                        ServerResponse
                                .notFound()
                                .buildAndAwait()
                    } else {
                        val aktørId = pdlResponse.identPdl?.data?.hentIdenter?.identer?.first()?.ident!!
                        val pdlResponseDto = PdlResponseDto(PdlPersonDto(norskIdent, aktørId))

                        ServerResponse
                                .ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValueAndAwait(pdlResponseDto)
                    }

                }  catch (case: IkkeTilgang) {
                    ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .buildAndAwait()
                }
            }
        }
    }

    private suspend fun ServerRequest.hentSøknad() = body(BodyExtractors.toMono(HentPerson::class.java)).awaitFirst()

}
