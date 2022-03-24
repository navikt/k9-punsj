 package no.nav.k9punsj.integrasjoner.gosys

import kotlinx.coroutines.reactive.awaitFirst
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.k9punsj.tilgangskontroll.AuthenticationHandler
import no.nav.k9punsj.PublicRoutes
import no.nav.k9punsj.RequestContext
import no.nav.k9punsj.SaksbehandlerRoutes
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.journalpost.IkkeTilgang
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.rest.web.JournalpostId
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
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
    private val journalpostService: JournalpostService,
    private val azureGraphService: IAzureGraphService
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
                        val journalpostInfo = journalpostService.hentJournalpostInfo(requestParameters.journalpostId)
                        if (journalpostInfo == null) {
                            logger.warn("Fant ikke journalpost.", keyValue("journalpost_id", requestParameters.journalpostId))
                            return@RequestContext ServerResponse
                                .status(HttpStatus.NOT_FOUND)
                                .buildAndAwait()
                        } else if (!journalpostInfo.kanOpprettesJournalføringsoppgave){
                            logger.warn("Kan kun opprette journalføringsoppgaver på inngående journalposter i status mottatt.", keyValue("journalpost_id", requestParameters.journalpostId))
                            return@RequestContext ServerResponse
                                .status(HttpStatus.CONFLICT)
                                .buildAndAwait()
                        }

                        val aktørid = hentIdenter.identer[0].ident
                        val (httpStatus, feil) = gosysOppgaveService.opprettOppgave(
                            aktørid = aktørid,
                            joarnalpostId = requestParameters.journalpostId,
                            gjelder = requestParameters.gjelder
                        )

                        if (feil != null) {
                            return@RequestContext ServerResponse
                                .status(httpStatus)
                                .json()
                                .bodyValueAndAwait(OasFeil(feil))
                        }
                        aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(
                            requestParameters.journalpostId,
                            erSendtInn = false,
                            ansvarligSaksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
                        )
                        journalpostService.settTilFerdig(requestParameters.journalpostId)

                        logger.info("Journalpost sendes til Gosys", keyValue("journalpost_id", requestParameters.journalpostId))

                        return@RequestContext ServerResponse
                            .status(HttpStatus.OK)
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
        val gjelder: Gjelder = Gjelder.Annet
    )
}
