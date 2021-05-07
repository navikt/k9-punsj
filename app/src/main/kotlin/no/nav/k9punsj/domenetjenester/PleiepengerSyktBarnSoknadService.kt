package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics.Companion.PLEIEPENGER_SYKT_BARN_TOPIC
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.JournalpostId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class PleiepengerSyktBarnSoknadService(
    val hendelseProducer: HendelseProducer,
    val journalpostRepository: JournalpostRepository,
    val søknadRepository: SøknadRepository,
    val innsendingClient: InnsendingClient,
    val aksjonspunktService: AksjonspunktService
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
    }

    internal suspend fun sendSøknad(søknad: Søknad, journalpostIder: MutableSet<JournalpostId>) {
        val dokumentfordelingMelding: String = toDokumentfordelingMelding(søknad, journalpostIder)

        hendelseProducer.sendMedOnSuccess(
            topicName = PLEIEPENGER_SYKT_BARN_TOPIC,
            data = dokumentfordelingMelding,
            key = søknad.søknadId.id
        ) {
           runBlocking {
               journalpostRepository.settBehandletFerdig(journalpostIder)
               søknadRepository.markerSomSendtInn(søknad.søknadId.id)
               aksjonspunktService.settUtførtForAksjonspunkterOgSendLukkOppgaveTilK9Los(journalpostIder.toList(),
                   Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.UTFØRT))
           }
        }

        // TODO: Fjern dobbel innsending
        innsendingClient.sendSøknad(
            søknadId = søknad.søknadId.id,
            søknad = søknad
        )
    }

    fun toDokumentfordelingMelding(søknad: Any, journalpostIder: MutableSet<JournalpostId>): String {
        // Midlertidig generering av meldings-JSON i påvente av et definert format.
        val om: ObjectMapper = objectMapper()
        val dokumentfordelingMelding: ObjectNode = om.createObjectNode()
        val data: ObjectNode = dokumentfordelingMelding.objectNode()
        data.set<JsonNode>("søknad", om.valueToTree(søknad))
        data.set<ArrayNode>("journalpostIder", om.valueToTree<ArrayNode>(journalpostIder))
        dokumentfordelingMelding.set<JsonNode>("data", data)
        return dokumentfordelingMelding.toString()
    }
}
