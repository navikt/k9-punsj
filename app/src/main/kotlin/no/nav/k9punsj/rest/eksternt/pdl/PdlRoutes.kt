package no.nav.k9punsj.rest.eksternt.pdl

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.journalpost.IkkeTilgang
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PdlRoutes::class.java)
    }

    internal object Urls {
        internal const val HentIdent = "/pdl/hentident/"
    }

    @Bean
    fun PdlRoutes() = Routes(authenticationHandler) {
        POST("/api${Urls.HentIdent}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val norskIdent = request.norskIdentRequest()
                try {
                    val pdlResponse = pdlService.identifikator(
                            fnummer = norskIdent
                    )
                    if (pdlResponse == null) {
                        ServerResponse
                                .notFound()
                                .buildAndAwait()
                    } else {
                        ServerResponse
                                .ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValueAndAwait(pdlResponse)
                    }

                }  catch (case: IkkeTilgang) {
                    ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .buildAndAwait()
                }
            }
        }
    }

    private suspend fun ServerRequest.norskIdentRequest() = body(BodyExtractors.toMono(NorskIdent::class.java)).awaitFirst()


}
