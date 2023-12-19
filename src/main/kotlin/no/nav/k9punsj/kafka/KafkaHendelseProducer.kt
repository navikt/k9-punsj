package no.nav.k9punsj.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaHendelseProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>
) : HendelseProducer {
    override fun send(topicName: String, data: String, key: String) {
        kafkaTemplate.send(topicName, key, data).exceptionally {
            throw IllegalStateException("Kunne ikke sende sende til topic: $topicName")
        }.thenAccept {
            val metadata = it.recordMetadata
            logger.info("Melding sendt OK på Topic=[$topicName], Key=[$key], Offset=[${metadata.offset()}, Partition=[${metadata.partition()}]")
        }
    }

    override fun sendMedOnSuccess(topicName: String, data: String, key: String, onSuccess: () -> Unit) {
        send(topicName, data, key)
        onSuccess.invoke()
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaHendelseProducer::class.java)
    }
}
