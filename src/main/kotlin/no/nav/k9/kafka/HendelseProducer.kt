package no.nav.k9.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9.JournalpostId
import no.nav.k9.søknad.JsonUtils
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.util.concurrent.ListenableFutureCallback
import java.util.*


@Configuration
class HendelseProducer {

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
        private val logger: Logger = LoggerFactory.getLogger(HendelseProducer::class.java)
    }

    @Bean
    fun producerFactory(): ProducerFactory<String?, String?>? {
        return DefaultKafkaProducerFactory(producerConfigs()!!)
    }

    @Bean
    fun producerConfigs(): Map<String, Any>? {
        val props: MutableMap<String, Any> = HashMap()
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        props.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId)
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
        if (username != null && !username.isEmpty()) {
            properties["security.protocol"] = "SASL_SSL"
            properties["sasl.mechanism"] = "PLAIN"
            properties[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = trustStorePath
            properties[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = trustStorePassword
        }
    }

    private fun setUsernameAndPassword(username: String?, password: String?, properties: MutableMap<String, Any>) {
        if (username != null && !username.isEmpty()
                && password != null && !password.isEmpty()) {
            val jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";"
            val jaasCfg = String.format(jaasTemplate, username, password)
            properties["sasl.jaas.config"] = jaasCfg
        }
    }

    fun sendTilKafkaTopic(topicName: String, søknad: Any, søknadId: String, journalpostIder: MutableSet<JournalpostId>) {
        // TODO K9: Håndter journalposter:
        val dokumentfordelingMelding: String = toDokumentfordelingMelding(søknad, journalpostIder)
        val future: ListenableFuture<SendResult<String?, String?>> = kafkaTemplate()!!.send(topicName, søknadId, dokumentfordelingMelding)
        future.addCallback(object : ListenableFutureCallback<SendResult<String?, String?>?> {
            override fun onSuccess(result: SendResult<String?, String?>?) {
                logger.info("Melding sendt på Kafka-topic: $topicName")
            }

            override fun onFailure(ex: Throwable) {
                //TODO: Feiler p.t. ikke innsending slik at feilen ikke blir synlig for saksbehandler
                logger.warn("Kunne ikke legge søknad på Kafka-topic $topicName : ${ex.message}")
                throw KafkaException("Kunne ikke sende sende søknad til topic: $topicName")
            }
        })
    }

    fun toDokumentfordelingMelding(søknad: Any, journalpostIder: MutableSet<JournalpostId>): String {
        // Midlertidig generering av meldings-JSON i påvente av et definert format.
        val om: ObjectMapper = JsonUtils.getObjectMapper()
        val dokumentfordelingMelding: ObjectNode = om.createObjectNode()
        val data: ObjectNode = dokumentfordelingMelding.objectNode()
        data.set<JsonNode>("søknad", om.valueToTree(søknad))
        data.set<ArrayNode>("journalpostIder", om.valueToTree<ArrayNode>(journalpostIder))
        dokumentfordelingMelding.set<JsonNode>("data", data)
        return dokumentfordelingMelding.toString()
    }
}
