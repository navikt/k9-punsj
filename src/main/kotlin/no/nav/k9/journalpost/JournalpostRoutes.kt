package no.nav.k9.journalpost

import no.nav.k9.AuthenticationHandler
import no.nav.k9.JournalpostId
import no.nav.k9.RequestContext
import no.nav.k9.Routes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.*
import kotlin.coroutines.coroutineContext

@Configuration
internal class JournalpostRoutes(
        private val authenticationHandler: AuthenticationHandler,
        private val journalpostService: JournalpostService
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(JournalpostRoutes::class.java)
        private const val JournalpostIdKey = "journalpost_id"
        private const val DokumentIdKey = "dokument_id"
    }

    internal object Urls {
        internal const val HenteJournalpostInfo = "/journalpost/{$JournalpostIdKey}"
        internal const val HenteDokument = "/journalpost/{$JournalpostIdKey}/dokument/{$DokumentIdKey}"
    }

    @Bean
    fun JournalpostRoutes() = Routes (authenticationHandler) {

        GET("/api${Urls.HenteJournalpostInfo}") { request ->
            RequestContext(coroutineContext, request) {
                try {
                    val journalpostInfo = journalpostService.hentJournalpostInfo(
                            journalpostId = request.journalpostId()
                    )
                    if (journalpostInfo == null) {
                        ServerResponse
                                .notFound()
                                .buildAndAwait()
                    } else {
                        ServerResponse
                                .ok()
                                .json()
                                .bodyValueAndAwait(journalpostInfo)
                    }

                } catch (cause: IkkeStøttetJournalpost) {
                    ServerResponse
                            .badRequest()
                            .buildAndAwait()
                } catch (case: IkkeTilgang) {
                    ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .buildAndAwait()
                }
            }
        }

        GET("/api${Urls.HenteDokument}") { request ->
            RequestContext(coroutineContext, request) {
                try {
                    val dokument = journalpostService.hentDokument(
                            journalpostId = request.journalpostId(),
                            dokumentId = request.dokumentId()
                    )

                    if (dokument == null) {
                        ServerResponse
                                .notFound()
                                .buildAndAwait()
                    } else {
                        ServerResponse
                                .ok()
                                .contentType(dokument.contentType)
                                .bodyValueAndAwait(dokument.dataBuffer)
                    }
                } catch (cause: IkkeTilgang) {
                    ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .buildAndAwait()
                }

            }
        }
    }

    private suspend fun ServerRequest.journalpostId() : JournalpostId = pathVariable(JournalpostIdKey)
    private suspend fun ServerRequest.dokumentId() : DokumentId = pathVariable(DokumentIdKey)

}