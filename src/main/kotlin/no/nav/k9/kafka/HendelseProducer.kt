package no.nav.k9.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class HendelseProducer @Autowired constructor(
        private val kafkaPropertiesUtil: KafkaPropertiesUtil
) {

    private val producer = KafkaProducer<String, String>(kafkaPropertiesUtil.opprettProperties())


    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(HendelseProducer::class.java)
    }

    fun sendKafkaMessage(topic: String, melding: String) {
        try {
            producer.send(ProducerRecord(topic, melding)).get()
            logger.info("Søknad lagt på kafka-topic $topic")
        } catch (kafkaException: KafkaException) {
            logger.warn("Innsending av melding feilet: ${kafkaException.message}")
            producer.abortTransaction()
        }
    }


}
