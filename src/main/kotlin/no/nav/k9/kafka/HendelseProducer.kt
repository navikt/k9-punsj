package no.nav.k9.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class HendelseProducer {

    @Autowired
    lateinit var kafkaPropertiesUtil: KafkaPropertiesUtil


    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(HendelseProducer::class.java)
    }

    //TODO: Ikke instansiere producer
    fun sendKafkaMessage(topic: String, melding: String) {
        val producer = KafkaProducer<String, String>(kafkaPropertiesUtil.opprettProperties())
        try {
            producer.send(ProducerRecord(topic, melding)).get()
            logger.info("Søknad lagt på kafka-topic $topic")
        } catch (kafkaException: KafkaException) {
            logger.warn("Innsending av melding feilet: ${kafkaException.message}")
            producer.abortTransaction()
        } finally {
            producer.flush()
            producer.close()
        }
    }


}
