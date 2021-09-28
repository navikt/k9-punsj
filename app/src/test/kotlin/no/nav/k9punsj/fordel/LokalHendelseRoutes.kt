package no.nav.k9punsj.fordel

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.LokalProfil
import no.nav.k9punsj.PublicRoutes
import no.nav.k9punsj.RequestContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import kotlin.coroutines.coroutineContext

@Configuration
@LokalProfil
class LokalHendelseRoutes(
    private val hendelseMottaker: HendelseMottaker) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(LokalHendelseRoutes::class.java)
    }

    internal object Urls {
        internal const val ProsesserHendelse = "/prosesserHendelse/"
    }

    @Bean
    fun prosesserHendelseRoute() = PublicRoutes {
        POST("/api${Urls.ProsesserHendelse}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val fordelPunsjEventDto = request.request()
                try {
                    hendelseMottaker.prosesser(fordelPunsjEventDto)
                    ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .buildAndAwait()

                } catch (e: Exception) {
                    ServerResponse
                        .status(HttpStatus.BAD_REQUEST)
                        .buildAndAwait()
                }
            }
        }
    }

    private suspend fun ServerRequest.request() =
        body(BodyExtractors.toMono(FordelPunsjEventDto::class.java)).awaitFirst()
}
