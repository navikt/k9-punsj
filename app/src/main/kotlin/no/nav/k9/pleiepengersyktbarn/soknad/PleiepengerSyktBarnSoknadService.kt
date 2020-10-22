package no.nav.k9.pleiepengersyktbarn.soknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9.JournalpostId
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class PleiepengerSyktBarnSoknadService @Autowired constructor(
        var hendelseProducer: HendelseProducer
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
        const val PLEIEPENGER_SYKT_BARN_TOPIC = "privat-punsjet-pleiepengesoknad"
    }

    internal suspend fun sendSøknad(søknad: PleiepengerBarnSøknad, journalpostIder: MutableSet<JournalpostId>) {
        val dokumentfordelingMelding: String = toDokumentfordelingMelding(søknad, journalpostIder)
        hendelseProducer.send(topicName = PLEIEPENGER_SYKT_BARN_TOPIC, data = dokumentfordelingMelding, key = søknad.søknadId.id)
    }


    fun toDokumentfordelingMelding(søknad: Any, journalpostIder: MutableSet<JournalpostId>): String {
        // Midlertidig generering av meldings-JSON i påvente av et definert format.
        val om: ObjectMapper = JsonUtils.getObjectMapper()
        val dokumentfordelingMelding: ObjectNode = om.createObjectNode()
        val data: ObjectNode = dokumentfordelingMelding.objectNode()
        data.set<JsonNode>("søknad", om.valueToTree(søknad))
        data.set<ArrayNode>("journalpostIder", om.valueToTree<ArrayNode>(journalpostIder))
        dokumentfordelingMelding.set<JsonNode>("data", data)
        return dokumentfordelingMelding.toString()
    }
}
