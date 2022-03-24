package no.nav.k9punsj.domenetjenester

import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.dokarkiv.SafDtos
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.SafGateway
import no.nav.k9punsj.metrikker.SøknadMetrikkService
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.domenetjenester.dto.JournalpostId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.coroutines.coroutineContext


@Service
class SoknadService(
    val journalpostRepository: JournalpostRepository,
    val søknadRepository: SøknadRepository,
    val innsendingClient: InnsendingClient,
    val aksjonspunktService: AksjonspunktService,
    val søknadMetrikkService: SøknadMetrikkService,
    val safGateway: SafGateway
) {

    internal suspend fun sendSøknad(
        søknad: Søknad,
        journalpostIder: MutableSet<JournalpostId>,
    ): Pair<HttpStatus, String>? {
        val journalpostIdListe = journalpostIder.toList()
        val journalposterHarIkkeBlittSendtInn = journalpostRepository.kanSendeInn(journalpostIdListe)
        val punsjetAvSaksbehandler = søknadRepository.hentSøknad(søknad.søknadId.id)?.endret_av!!.replace("\"","")

        return if (journalposterHarIkkeBlittSendtInn) {
            val journalposterMedTypeUtgaaende = safGateway.hentJournalposter(journalpostIdListe)
                .filterNotNull()
                .filter { it.journalposttype.equals(SafDtos.JournalpostType.U) }
                .map { it.journalpostId }
                .toSet()

            if(journalposterMedTypeUtgaaende.isNotEmpty()) {
                return Pair(HttpStatus.CONFLICT, "Journalpost type Utgående ikke støttet: $journalposterMedTypeUtgaaende")
            }

            try {
                innsendingClient.sendSøknad(
                    søknadId = søknad.søknadId.id,
                    søknad = søknad,
                    correlationId = coroutineContext.hentCorrelationId(),
                    tilleggsOpplysninger = mapOf(PunsjetAvSaksbehandler to punsjetAvSaksbehandler)
                )
            } catch (e: Exception) {
                logger.error("Feil vid innsending av søknad for journalpostIder: ${søknad.journalposter}")
                return Pair(HttpStatus.INTERNAL_SERVER_ERROR, printStackTrace(e))
            }

            leggerVedPayload(søknad, journalpostIder)
            journalpostRepository.settAlleTilFerdigBehandlet(journalpostIdListe)
            logger.info("Punsj har market disse journalpostIdene $journalpostIder som ferdigbehandlet")
            søknadRepository.markerSomSendtInn(søknad.søknadId.id)

            aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(
                journalpostIder,
                erSendtInn = true,
                ansvarligSaksbehandler = punsjetAvSaksbehandler
            )

            søknadMetrikkService.publiserMetrikker(søknad)
            null
        } else {
            Pair(HttpStatus.CONFLICT, "En eller alle journalpostene${journalpostIder} har blitt sendt inn fra før")
        }
    }

    private fun printStackTrace(e: Exception): String {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    private suspend fun leggerVedPayload(
        søknad: Søknad,
        journalpostIder: MutableSet<JournalpostId>,
    ) {
        val writeValueAsString = objectMapper().writeValueAsString(søknad)

        journalpostIder.forEach {
            val journalpost = journalpostRepository.hent(it)
            val medPayload = journalpost.copy(payload = writeValueAsString)
            journalpostRepository.lagre(medPayload) {
                medPayload
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SoknadService::class.java)
        private const val PunsjetAvSaksbehandler = "saksbehandler"
    }
}
