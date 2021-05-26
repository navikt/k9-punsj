package no.nav.k9punsj.domenetjenester

import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.rest.web.JournalpostId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import kotlin.coroutines.coroutineContext


@Service
class PleiepengerSyktBarnSoknadService(
    val hendelseProducer: HendelseProducer,
    val journalpostRepository: JournalpostRepository,
    val søknadRepository: SøknadRepository,
    val innsendingClient: InnsendingClient,
    val aksjonspunktService: AksjonspunktService,
) {

    internal suspend fun sendSøknad(
        søknad: Søknad,
        journalpostIder: MutableSet<JournalpostId>,
    ): Pair<HttpStatus, String>? {
        return if (journalpostRepository.kanSendeInn(journalpostIder.toList())) {
            try {
                innsendingClient.sendSøknad(
                    søknadId = søknad.søknadId.id,
                    søknad = søknad,
                    correlationId = coroutineContext.hentCorrelationId()
                )
            } catch (e: Exception) {
                return Pair(HttpStatus.INTERNAL_SERVER_ERROR, e.message!!)
            }
            journalpostRepository.settAlleTilFerdigBehandlet(journalpostIder.toList())
            logger.info("Punsj har market disse journalpostIdene $journalpostIder som ferdigbehandlet")
            søknadRepository.markerSomSendtInn(søknad.søknadId.id)
            aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostIder.toList(), true)
            null
        } else {
            Pair(HttpStatus.CONFLICT, "En eller alle journalpostene${journalpostIder} har blitt sendt inn fra før")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
    }
}
