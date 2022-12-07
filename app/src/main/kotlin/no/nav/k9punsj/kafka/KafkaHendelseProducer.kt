package no.nav.k9punsj.kafka

import no.nav.k9punsj.IkkeTestProfil
import no.nav.k9punsj.configuration.KafkaConfig.Companion.AIVEN
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.util.concurrent.ListenableFutureCallback

@Component
@IkkeTestProfil
class KafkaHendelseProducer(
    @Qualifier(AIVEN) private val kafkaTemplate: KafkaTemplate<String, String>
) : HendelseProducer {
    override fun send(topicName: String, data: String, key: String) {
        val future: ListenableFuture<SendResult<String?, String?>> = kafkaTemplate.send(topicName, key, data)
        future.addCallback(object : ListenableFutureCallback<SendResult<String?, String?>?> {
            override fun onSuccess(result: SendResult<String?, String?>?) {
                val recordmetadata = result?.recordMetadata
                logger.info("Melding sendt OK på Topic=[$topicName], Key=[$key], Offset=[${recordmetadata?.offset()}, Partition=[${recordmetadata?.partition()}]")
            }

            override fun onFailure(ex: Throwable) {
                // TODO: Feiler p.t. ikke innsending slik at feilen ikke blir synlig for saksbehandler
                logger.warn("Kunne ikke legge søknad på Kafka-topic $topicName : ${ex.message}")
                throw IllegalStateException("Kunne ikke sende sende til topic: $topicName")
            }
        })
    }

    override fun sendMedOnSuccess(topicName: String, data: String, key: String, onSuccess: () -> Unit) {
        val future: ListenableFuture<SendResult<String?, String?>> = kafkaTemplate.send(topicName, key, data)
        future.addCallback(object : ListenableFutureCallback<SendResult<String?, String?>?> {
            override fun onSuccess(result: SendResult<String?, String?>?) {
                val recordmetadata = result?.recordMetadata
                logger.info("Melding sendt OK på Topic=[$topicName], Key=[$key], Offset=[${recordmetadata?.offset()}, Partition=[${recordmetadata?.partition()}]")
                onSuccess.invoke()
            }

            override fun onFailure(ex: Throwable) {
                // TODO: Feiler p.t. ikke innsending slik at feilen ikke blir synlig for saksbehandler
                logger.warn("Kunne ikke legge søknad på Kafka-topic $topicName : ${ex.message}")
                throw IllegalStateException("Kunne ikke sende sende til topic: $topicName")
            }
        })
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaHendelseProducer::class.java)
    }
}
