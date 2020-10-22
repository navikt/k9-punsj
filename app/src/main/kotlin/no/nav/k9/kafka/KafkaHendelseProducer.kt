package no.nav.k9.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.util.concurrent.ListenableFutureCallback
import java.util.*


@Configuration
@Profile("!test")
class KafkaHendelseProducer: HendelseProducer {

    @Value("\${kafka.bootstrap.server}")
    val bootstrapServers = ""
    @Value("\${kafka.clientId}")
    val clientId = ""
    @Value("\${systembruker.username}")
    val username = ""
    @Value("\${systembruker.password}")
    val password = ""
    @Value("\${javax.net.ssl.trustStore}")
    val trustStorePath = ""
    @Value("\${javax.net.ssl.trustStorePassword}")
    val trustStorePassword = ""

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaHendelseProducer::class.java)
    }

    @Bean
    fun producerFactory(): ProducerFactory<String?, String?>? {
        return DefaultKafkaProducerFactory(producerConfigs()!!)
    }

    @Bean
    fun producerConfigs(): Map<String, Any>? {
        val props: MutableMap<String, Any> = HashMap()
        props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[CommonClientConfigs.CLIENT_ID_CONFIG] = clientId
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java

        setSecurity(username, props)
        setUsernameAndPassword(username, password, props)

        return props
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String?, String?>? {
        return KafkaTemplate(producerFactory()!!)
    }

    private fun setSecurity(username: String?, properties: MutableMap<String, Any>) {
        if (username != null && username.isNotEmpty()) {
            properties["security.protocol"] = "SASL_SSL"
            properties["sasl.mechanism"] = "PLAIN"
            properties[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = trustStorePath
            properties[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = trustStorePassword
        }
    }

    private fun setUsernameAndPassword(username: String?, password: String?, properties: MutableMap<String, Any>) {
        if (username != null && username.isNotEmpty()
                && password != null && password.isNotEmpty()) {
            val jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";"
            val jaasCfg = String.format(jaasTemplate, username, password)
            properties["sasl.jaas.config"] = jaasCfg
        }
    }


    override fun send(topicName: String, data: String, key: String) {
        val future: ListenableFuture<SendResult<String?, String?>> = kafkaTemplate()!!.send(topicName, key, data)
        future.addCallback(object : ListenableFutureCallback<SendResult<String?, String?>?> {
            override fun onSuccess(result: SendResult<String?, String?>?) {
                logger.info("Melding sendt på Kafka-topic: $topicName")
            }

            override fun onFailure(ex: Throwable) {
                //TODO: Feiler p.t. ikke innsending slik at feilen ikke blir synlig for saksbehandler
                logger.warn("Kunne ikke legge søknad på Kafka-topic $topicName : ${ex.message}")
                throw KafkaException("Kunne ikke sende sende til topic: $topicName")
            }
        })
    }
}
