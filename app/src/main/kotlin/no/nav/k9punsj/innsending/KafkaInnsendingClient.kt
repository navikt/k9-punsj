package no.nav.k9punsj.innsending

import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.configuration.KafkaConfig.Companion.AIVEN
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@StandardProfil
@ConditionalOnProperty("innsending.rest.enabled", havingValue = "false", matchIfMissing = true)
class KafkaInnsendingClient(
    @Qualifier(AIVEN) kafkaBaseProperties: Map<String, Any>,
    @Value("\${no.nav.kafka.k9_rapid.topic}") private val k9rapidTopic: String
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

    override suspend fun send(pair: Pair<String, String>) {
        val (key, value) = pair
        kotlin.runCatching {
            kafkaProducer.send(ProducerRecord(k9rapidTopic, key, value)).get()
        }.onSuccess { metadata ->
            logger.info("Innsending OK, Key=[$key], ClientId=[$clientId], Topic=[${metadata.topic()}], Offset=[${metadata.offset()}], Partition=[${metadata.partition()}]")
        }.onFailure { throwable ->
            throw IllegalStateException("Feil ved innsending, Key=[$key], ClientId=[$clientId], Topic=[$k9rapidTopic]", throwable)
        }
    }

    override fun toString() = "KafkaInnsendingClient: Innsendinger sendes på Topic=[$k9rapidTopic], ClientId=[$clientId]"

    private companion object {
        private val logger = LoggerFactory.getLogger(KafkaInnsendingClient::class.java)
    }
}
