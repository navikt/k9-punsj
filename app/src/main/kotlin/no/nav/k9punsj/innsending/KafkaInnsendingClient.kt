package no.nav.k9punsj.innsending

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.Properties

class KafkaInnsendingClient(kafkaProperties: Properties) : InnsendingClient {
    private val clientId = kafkaProperties.getValue(ProducerConfig.CLIENT_ID_CONFIG)
    private val kafkaProducer = KafkaProducer(
        kafkaProperties,
        StringSerializer(),
        StringSerializer()
    )

    override fun send(pair: Pair<String, String>) {
        val (key, value) = pair
        kotlin.runCatching {
            kafkaProducer.send(ProducerRecord(TOPIC, key, value)).get()
        }.onSuccess { metadata ->
            logger.info("Innsending OK, Key=[$key], ClientId=[$clientId], Topic=[${metadata.topic()}], Offset=[${metadata.offset()}], Partition=[${metadata.partition()}]")
        }.onFailure { throwable ->
            throw IllegalStateException("Feil ved innsending, Key=[$key], ClientId=[$clientId], Topic=[$TOPIC]", throwable)
        }
    }

    override fun toString() = "KafkaInnsendingClient: Innsendinger sendes på Topic=[$TOPIC], ClientId=[$clientId]"

    private companion object {
        private val logger = LoggerFactory.getLogger(KafkaInnsendingClient::class.java)
        private const val TOPIC = "omsorgspenger.k9-rapid-v2"
    }
}