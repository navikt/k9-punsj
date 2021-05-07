package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.db.datamodell.SøknadId
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
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
    val innsendingClient: InnsendingClient
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
        const val PLEIEPENGER_SYKT_BARN_TOPIC = "privat-punsjet-pleiepengesoknad"
        lateinit var journalposter: MutableSet<JournalpostId>
        lateinit var sId: SøknadId
    }

    internal suspend fun sendSøknad(søknad: Søknad, journalpostIder: MutableSet<JournalpostId>) {
        val dokumentfordelingMelding: String = toDokumentfordelingMelding(søknad, journalpostIder)
        journalposter = journalpostIder
        sId = søknad.søknadId.id

        hendelseProducer.sendMedOnSuccess(topicName = PLEIEPENGER_SYKT_BARN_TOPIC,
            data = dokumentfordelingMelding,
            key = søknad.søknadId.id,
            onSuccess = onSuccess
        )

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

    val onSuccess = {
        runBlocking {
            journalpostRepository.settBehandletFerdig(journalposter)
            søknadRepository.markerSomSendtInn(sId)
        }
    }
}
