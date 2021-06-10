package no.nav.k9punsj.innsending

import no.nav.k9punsj.kafka.KafkaConfig.Companion.AIVEN
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test & !local")
class KafkaInnsendingClient(
    @Qualifier(AIVEN) kafkaBaseProperties: Map<String, Any>
) : InnsendingClient {
    private val clientId = kafkaBaseProperties.getValue(CommonClientConfigs.CLIENT_ID_CONFIG)
    private val kafkaProducer = KafkaProducer(
        kafkaBaseProperties.toMutableMap().also {
            it[ProducerConfig.ACKS_CONFIG] = "1"
            it[ProducerConfig.LINGER_MS_CONFIG] = "0"
            it[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = "1"
        },
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