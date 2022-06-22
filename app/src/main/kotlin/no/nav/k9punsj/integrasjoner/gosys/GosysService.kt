package no.nav.k9punsj.integrasjoner.gosys

import net.logstash.logback.argument.StructuredArguments
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.openapi.OasFeil
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.json

@Service
internal class GosysService(
    private val pdlService: PdlService,
    private val journalpostService: JournalpostService,
    private val oppgaveGateway: OppgaveGateway,
    private val aksjonspunktService: AksjonspunktService,
    private val azureGraphService: IAzureGraphService
) {

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(GosysService::class.java)
    }

    internal suspend fun opprettJournalforingsOppgave(
        oppgaveRequest: GosysRoutes.GosysOpprettJournalføringsOppgaveRequest
    ): ServerResponse {
        val identifikator = pdlService.identifikator(oppgaveRequest.norskIdent)

        val hentIdenter = identifikator?.identPdl?.data?.hentIdenter

        if (hentIdenter == null) {
            logger.warn("Kunne ikke finne person i pdl")
            return ServerResponse
                .notFound()
                .buildAndAwait()
        }

        val journalpostInfo = journalpostService.hentJournalpostInfo(oppgaveRequest.journalpostId)
            ?: return ServerResponse.status(HttpStatus.NOT_FOUND).buildAndAwait()
                .also { logger.warn("Kunne ikke finne journalpost med id {}", oppgaveRequest.journalpostId) }

        if (!journalpostInfo.kanOpprettesJournalføringsoppgave) {
            logger.warn(
                "Kan kun opprette journalføringsoppgaver på inngående journalposter i status mottatt.",
                StructuredArguments.keyValue("journalpost_id", oppgaveRequest.journalpostId)
            )
            return ServerResponse
                .status(HttpStatus.CONFLICT)
                .buildAndAwait()
        }

        val aktørid = hentIdenter.identer[0].ident
        val (httpStatus, feil) = oppgaveGateway.opprettOppgave(
            aktørid = aktørid,
            joarnalpostId = oppgaveRequest.journalpostId,
            gjelder = oppgaveRequest.gjelder
        )

        if (feil != null) {
            return ServerResponse
                .status(httpStatus)
                .json()
                .bodyValueAndAwait(OasFeil(feil))
        }
        aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(
            journalpostId = oppgaveRequest.journalpostId,
            erSendtInn = false,
            ansvarligSaksbehandler = azureGraphService.hentIdentTilInnloggetBruker()
        )
        journalpostService.settTilFerdig(oppgaveRequest.journalpostId)

        logger.info(
            "Journalpost sendes til Gosys",
            StructuredArguments.keyValue("journalpost_id", oppgaveRequest.journalpostId)
        )

        return ServerResponse
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .buildAndAwait()
    }
}
