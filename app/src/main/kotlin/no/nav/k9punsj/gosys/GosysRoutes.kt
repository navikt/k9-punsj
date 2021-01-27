package no.nav.k9punsj.gosys

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.JournalpostId
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.journalpost.IkkeTilgang
import no.nav.k9punsj.person.Person
import no.nav.k9punsj.rest.pdl.PdlService
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
internal class GosysRoutes(
        private val authenticationHandler: AuthenticationHandler,
        private val gosysOppgaveService: GosysOppgaveService,
        private val pdlService: PdlService
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(GosysRoutes::class.java)
    }

    internal object Urls {
        internal const val OpprettJournalføringsoppgave = "/gosys/opprettJournalforingsoppgave/"
    }

    @Bean
    fun GosysRoutes() = Routes(authenticationHandler) {
        POST("/api${Urls.OpprettJournalføringsoppgave}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val requestParameters = request.request()
                try {
                    val identifikator = pdlService.identifikator(requestParameters.norskIdent.personIdent.ident)
                    val hentIdenter = identifikator?.aktøridPdl?.data?.hentIdenter
                    if (hentIdenter == null) {
                        logger.warn("Kunne ikke finne person i pdl")
                        ServerResponse
                                .notFound()
                                .buildAndAwait()
                    } else {
                        val aktørid = hentIdenter.identer[0].ident
                        val response = gosysOppgaveService.opprettOppgave(
                                aktørid = aktørid, joarnalpostId = requestParameters.journalpostId
                        )
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

                    }

                } catch (case: IkkeTilgang) {
                    ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .buildAndAwait()
                }
            }
        }

    }

    private suspend fun ServerRequest.request() = body(BodyExtractors.toMono(GosysOpprettJournalføringsOppgaveRequest::class.java)).awaitFirst()

    data class GosysOpprettJournalføringsOppgaveRequest(
            val norskIdent: Person,
            val journalpostId: JournalpostId
    )
}
