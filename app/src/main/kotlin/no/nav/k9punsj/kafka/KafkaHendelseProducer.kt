package no.nav.k9punsj.kafka

import no.nav.k9punsj.IkkeTestProfil
import no.nav.k9punsj.configuration.KafkaConfig.Companion.AIVEN
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@IkkeTestProfil
class KafkaHendelseProducer(
    @Qualifier(AIVEN) private val kafkaTemplate: KafkaTemplate<String, String>
) : HendelseProducer {
    override fun send(topicName: String, data: String, key: String) {
        kafkaTemplate.send(topicName, key, data).also {
            when(it.isDone) {
                true -> {
                    val recordmetadata = it.get().recordMetadata
                    logger.info("Melding sendt OK på Topic=[$topicName], Key=[$key], Offset=[${recordmetadata?.offset()}, Partition=[${recordmetadata?.partition()}]")
                }
                false -> {
                    logger.warn("Kunne ikke legge søknad på Kafka-topic $topicName : $topicName")
                    throw IllegalStateException("Kunne ikke sende sende til topic: $topicName")
                }
            }
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
