package no.nav.k9punsj.fordel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.kafka.KafkaConfig.Companion.AIVEN_CONTAINER_FACTORY
import no.nav.k9punsj.kafka.KafkaConfig.Companion.ON_PREM_CONTAINER_FACTORY
import no.nav.k9punsj.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.io.IOException

@Service
@Profile("!test")
class FordelConsumer @Autowired constructor(val hendelseMottaker: HendelseMottaker) {

    @KafkaListener(
        topics = [ON_PREM_TOPIC],
        groupId = "k9-punsj-3",
        properties = ["auto.offset.reset:earliest"],
        containerFactory = ON_PREM_CONTAINER_FACTORY
    )
    @Throws(IOException::class)
    fun consumeOnPremPunsjbarJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(ON_PREM_TOPIC)) }
    }

    @KafkaListener(
        topics = [AIVEN_TOPIC],
        groupId = "k9-punsj-1",
        properties = ["auto.offset.reset:earliest"],
        containerFactory = AIVEN_CONTAINER_FACTORY
    )
    @Throws(IOException::class)
    fun consumeAivenPunsjbarJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(AIVEN_TOPIC)) }
    }

    internal companion object {
        private const val ON_PREM_TOPIC = "privat-k9-fordel-journalforing-v1"
        private const val AIVEN_TOPIC = "k9saksbehandling.punsjbar-journalpost"

        private val logger: Logger = LoggerFactory.getLogger(FordelConsumer::class.java)
        private val objectMapper = objectMapper()
        private val maskertText = TextNode("***")

        internal fun String.somFordelPunsjEventDto(topic: String) : FordelPunsjEventDto {
            val maskert = kotlin.runCatching { objectMapper.readTree(this).let { it as ObjectNode }.also {
                if (it.hasNonNull("aktørId")) {
                    it.replace("aktørId", maskertText)
                }
            }.toString()}.fold(onSuccess = {it}, onFailure = {
                throw IllegalStateException("Mottatt melding på Topic=[$topic] som ikke er gyldig JSON. Melding=$this", it)
            })

            return kotlin.runCatching { objectMapper.readValue<FordelPunsjEventDto>(this).also {
                logger.info("Mottatt punsjbar journalpost på Topic=[$topic], PunsjbarJournalpost=$maskert")
            }}.fold(onSuccess = {it}, onFailure = {
                throw IllegalStateException("Mottatt melding på Topic=[$topic] som ikke kan deserialisers. Melding=$maskert", it)
            })
        }
    }
}
