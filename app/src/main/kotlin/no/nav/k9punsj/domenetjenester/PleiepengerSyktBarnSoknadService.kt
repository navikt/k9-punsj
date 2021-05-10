package no.nav.k9punsj.domenetjenester

import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.rest.web.JournalpostId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.coroutines.coroutineContext


@Service
class PleiepengerSyktBarnSoknadService(
    val hendelseProducer: HendelseProducer,
    val journalpostRepository: JournalpostRepository,
    val søknadRepository: SøknadRepository,
    val innsendingClient: InnsendingClient,
    val aksjonspunktService: AksjonspunktService) {

    internal suspend fun sendSøknad(søknad: Søknad, journalpostIder: MutableSet<JournalpostId>) {
        innsendingClient.sendSøknad(
            søknadId = søknad.søknadId.id,
            søknad = søknad,
            correlationId = coroutineContext.hentCorrelationId()
        )
        kotlin.runCatching { journalpostRepository.settBehandletFerdig(journalpostIder) }.onFailure { throwable ->
            // TODO: Burde ikke fortsette når denne feilen oppstår.
            logger.error("Feil ved setting av behandling ferdig for JournalpostIder=$journalpostIder", throwable)
        }
        søknadRepository.markerSomSendtInn(søknad.søknadId.id)
        aksjonspunktService.settUtførtForAksjonspunkterOgSendLukkOppgaveTilK9Los(journalpostIder.toList(),
            Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.UTFØRT))
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
    }

}
