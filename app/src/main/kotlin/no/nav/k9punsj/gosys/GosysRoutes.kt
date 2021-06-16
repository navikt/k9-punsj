package no.nav.k9punsj.gosys

import kotlinx.coroutines.reactive.awaitFirst
import no.nav.k9punsj.AuthenticationHandler
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.Routes
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.journalpost.IkkeTilgang
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.rest.web.openapi.OasFeil
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
    private val gosysOppgaveService: GosysOppgaveService,
    private val pdlService: PdlService,
    private val aksjonspunktService: AksjonspunktService,
    private val journalpostService: JournalpostService

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
                    val identifikator = pdlService.identifikator(requestParameters.norskIdent)
                    val hentIdenter = identifikator?.identPdl?.data?.hentIdenter
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

                        if (response.second != null) {
                            return@RequestContext ServerResponse
                                .status(response.first)
                                .json()
                                .bodyValueAndAwait(OasFeil(response.second!!))
                        }
                        aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(requestParameters.journalpostId, false)
                        journalpostService.settTilFerdig(requestParameters.journalpostId)

                        return@RequestContext ServerResponse
                            .status(response.first)
                            .contentType(MediaType.APPLICATION_JSON)
                            .buildAndAwait()
                    }

                } catch (case: IkkeTilgang) {
                    ServerResponse
                        .status(HttpStatus.FORBIDDEN)
                        .buildAndAwait()
                }
            }
        }

    }

    private suspend fun ServerRequest.request() =
        body(BodyExtractors.toMono(GosysOpprettJournalføringsOppgaveRequest::class.java)).awaitFirst()

    data class GosysOpprettJournalføringsOppgaveRequest(
        val norskIdent: NorskIdent,
        val journalpostId: JournalpostId,
    )
}
