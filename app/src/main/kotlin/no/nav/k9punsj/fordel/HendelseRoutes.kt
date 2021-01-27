package no.nav.k9punsj.fordel

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.JournalpostId
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
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
internal class HendelseRoutes(
        private val hendelseMottaker: HendelseMottaker,
        private val authenticationHandler: AuthenticationHandler,
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(HendelseRoutes::class.java)
    }

    internal object Urls {
        internal const val ProsesserHendelse = "/prosesserHendelse/"
    }

    @Bean
    fun HendelseRoutes() = Routes(authenticationHandler) {
        POST("/api${Urls.ProsesserHendelse}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val params = request.request()
                try {
                    val response = hendelseMottaker.prosesser(params.journalpostId, params.aktørId)
                    if (response == null) {
                        ServerResponse
                                .notFound()
                                .buildAndAwait()
                    } else {
                        ServerResponse
                                .ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValueAndAwait(response)
                    }
                    } catch (case: IkkeTilgang) {
                    ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .buildAndAwait()
                }
            }
        }

    }
    private suspend fun ServerRequest.request() = body(BodyExtractors.toMono(FordelPunsjEventDto::class.java)).awaitFirst()

    data class FordelPunsjEventDto(
            val aktørId: no.nav.k9punsj.AktørId?,
            val journalpostId: JournalpostId
    )
}
