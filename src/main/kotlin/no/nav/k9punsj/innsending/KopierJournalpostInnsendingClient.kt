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
import org.springframework.stereotype.Component

@Component
@StandardProfil
class KopierJournalpostInnsendingClient(
    @Qualifier(AIVEN) kafkaBaseProperties: Map<String, Any>,
    @Value("\${no.nav.kafka.k9_rapid.topic}") private val k9rapidTopic: String
) {
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

    suspend fun send(pair: Pair<String, String>) {
        val (key, value) = pair
        kotlin.runCatching {
            kafkaProducer.send(ProducerRecord(k9rapidTopic, key, value)).get()
        }.onSuccess { metadata ->
            logger.info("Innsending OK, Key=[$key], ClientId=[$clientId], Topic=[${metadata.topic()}], Offset=[${metadata.offset()}], Partition=[${metadata.partition()}]")
        }.onFailure { throwable ->
            throw IllegalStateException("Feil ved innsending, Key=[$key], ClientId=[$clientId], Topic=[$k9rapidTopic]", throwable)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(KopierJournalpostInnsendingClient::class.java)
    }
}
