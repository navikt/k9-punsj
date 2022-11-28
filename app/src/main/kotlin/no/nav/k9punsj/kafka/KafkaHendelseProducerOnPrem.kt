package no.nav.k9punsj.kafka

import no.nav.k9punsj.IkkeTestProfil
import no.nav.k9punsj.kafka.KafkaConfig.Companion.ON_PREM
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@IkkeTestProfil
class KafkaHendelseProducerOnPrem(
    @Qualifier(ON_PREM) private val kafkaTemplate: KafkaTemplate<String, String>
) : HendelseProducerOnprem {
    override fun send(topicName: String, data: String, key: String) {
        kafkaTemplate.send(topicName, key, data).also {
            when(it.isDone) {
                true -> logger.info("Melding sendt på Kafka-topic: ${it.get().recordMetadata.topic()}")
                false -> {
                    logger.warn("Kunne ikke legge søknad på Kafka-topic $topicName : $topicName")
                    throw KafkaException("Kunne ikke sende sende til topic: $topicName")
                }
            }
        }
    }

    override fun sendMedOnSuccess(topicName: String, data: String, key: String, onSuccess: () -> Unit) {
        send(topicName, data, key)
        onSuccess.invoke()
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaHendelseProducerOnPrem::class.java)
    }
}
