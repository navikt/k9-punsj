package no.nav.k9.pleiepengersyktbarn.soknad

import no.nav.k9.*
import no.nav.k9.mappe.Mappe
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.springframework.beans.factory.annotation.Value
import java.util.*


@Service
class KafkaPropertiesHelper {
    @Value("\${kafka.bootstrap.server}")
    val server= ""
    @Value("\${kafka.ack}")
    val ack="1"
    @Value("\${kafka.retries}")
    val retries="0"
    @Value("\${kafka.batch.size}")
    val size="33554432"
    @Value("\${kafka.linger.ms}")
    val ms="1"
    @Value("\${kafka.buffer.memory}")
    val memory="33554432"
    @Value("\${kafka.key.serializer}")
    val key="org.apache.kafka.common.serialization.StringSerializer"
    @Value("\${kafka.value.serializer}")
    val value="org.apache.kafka.common.serialization.StringSerializer"
    @Value("\${kafka.security.protocol}")
    val protocol="SASL_SSL"
    @Value("\${kafka.sasl.mechanism}")
    val mechanism="SASL_SSL"
    @Value("\${kafka.sslconfig.truststore}")
    val truststore="/Users/jankaspermartinsen/.modig/truststore.jks"
    @Value("\${kafka.sslconfig.password}")
    val password="changeit"
}

@Service
internal class PleiepengerSyktBarnSoknadService {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PleiepengerSyktBarnSoknadService::class.java)
    }

    @Autowired
    lateinit var kafkapropertieshelper : KafkaPropertiesHelper

    internal suspend fun sendSøknad(
            norskIdent: NorskIdent,
            mappe: Mappe
    ) {

        sendKafkaMessage("privat-omsorgspengesoknad-journalfort")

        // TODO: Legge på en kafka-topic k9-fordel håndterer.
        logger.info("sendSøknad")
        logger.info("NorskIdent=$norskIdent")
        logger.info("Mappe=$mappe")
    }

    fun sendKafkaMessage(topic:String){

        val props = Properties()
        props["bootstrap.servers"] = kafkapropertieshelper.server
        props["acks"] = kafkapropertieshelper.ack
        props["retries"] = kafkapropertieshelper.retries
        props["batch.size"] = kafkapropertieshelper.size
        props["linger.ms"] = kafkapropertieshelper.ms
        props["buffer.memory"] = kafkapropertieshelper.memory
        props["key.serializer"] = kafkapropertieshelper.key
        props["value.serializer"] = kafkapropertieshelper.value
        props["security.protocol"] = kafkapropertieshelper.protocol
        props["sasl.mechanism"] = kafkapropertieshelper.mechanism
        props["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required\n" +
                "username=\"vtp\"\n" +
                "password=\"vtp\";"
        props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = kafkapropertieshelper.truststore
        props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = kafkapropertieshelper.password

        val producer = KafkaProducer<String, String>(props)
        try {
            producer.send(ProducerRecord(topic, "{}")).get()
        }catch (e:Exception){

        }finally {
            producer.flush()
            producer.close()
        }
    }
}
