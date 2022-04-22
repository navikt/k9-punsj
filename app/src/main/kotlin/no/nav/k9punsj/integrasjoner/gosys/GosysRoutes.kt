 package no.nav.k9punsj.integrasjoner.gosys

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.PublicRoutes
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.journalpost.IkkeTilgang
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import kotlin.coroutines.coroutineContext

@Configuration
internal class GosysRoutes(
    private val authenticationHandler: AuthenticationHandler,
    private val gosysService: GosysService,
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(GosysRoutes::class.java)
    }

    internal object Urls {
        internal const val OpprettJournalføringsoppgave = "/gosys/opprettJournalforingsoppgave/"
        internal const val Gjelder = "/gosys/gjelder"
    }

    @Bean
    fun PublicGosysRoutes() = PublicRoutes {
        GET("/api${Urls.Gjelder}") {
            return@GET ServerResponse
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(Gjelder.JSON)
        }
    }

    @Bean
    fun GosysRoutes() = SaksbehandlerRoutes(authenticationHandler) {
        POST("/api${Urls.OpprettJournalføringsoppgave}", contentType(MediaType.APPLICATION_JSON)) { request ->
            RequestContext(coroutineContext, request) {
                val oppgaveRequest = request.mapOppgaveRequest()

                return@RequestContext try {
                    gosysService.opprettJournalforingsOppgave(oppgaveRequest)
                } catch (case: IkkeTilgang) {
                    ServerResponse.status(HttpStatus.FORBIDDEN).buildAndAwait()
                }
            }
        }
    }

    private suspend fun ServerRequest.mapOppgaveRequest() =
        body(BodyExtractors.toMono(GosysOpprettJournalføringsOppgaveRequest::class.java)).awaitFirst()

    data class GosysOpprettJournalføringsOppgaveRequest(
        val norskIdent: String,
        val journalpostId: String,
        val gjelder: Gjelder = Gjelder.Annet
    )
}
