package no.nav.k9punsj.innsending

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetAddress
import java.util.*

@Configuration
class InnsendingClientConfiguration {

    @Bean
    fun innsendingClient() = when (val kafkaProperties = kafkaPropertiesOrNull()) {
        null -> LoggingInnsendingClient()
        else -> KafkaInnsendingClient(kafkaProperties)
    }

    private fun kafkaPropertiesOrNull(environment: Map<String, String> = System.getenv()): Properties? {
        val kafkaEnvironmentVariables = environment.filterKeys { it in RequiredKafkaEnvironmentVariables  }
        if (!kafkaEnvironmentVariables.keys.containsAll(RequiredKafkaEnvironmentVariables)) {
            logger.warn("Mangler EnvironmentVariables=[${RequiredKafkaEnvironmentVariables.minus(kafkaEnvironmentVariables.keys)}]")
            return null
        }

        return Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaEnvironmentVariables.getValue(KAFKA_BOKERS))
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaEnvironmentVariables.getValue(KAFKA_TRUSTSTORE_PATH))
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaEnvironmentVariables.getValue(KAFKA_CREDSTORE_PASSWORD))
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, kafkaEnvironmentVariables.getValue(KAFKA_KEYSTORE_PATH))
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kafkaEnvironmentVariables.getValue(KAFKA_CREDSTORE_PASSWORD))
            // Producer Properties
            put(ProducerConfig.CLIENT_ID_CONFIG, "${CLIENT_ID_PREFIX}${InetAddress.getLocalHost().hostName}")
            put(ProducerConfig.ACKS_CONFIG, "1")
            put(ProducerConfig.LINGER_MS_CONFIG, "0")
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        }
    }

    private companion object {
        private const val KAFKA_BOKERS = "KAFKA_BROKERS"
        private const val KAFKA_TRUSTSTORE_PATH = "KAFKA_TRUSTSTORE_PATH"
        private const val KAFKA_KEYSTORE_PATH = "KAFKA_KEYSTORE_PATH"
        private const val KAFKA_CREDSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD"
        private val RequiredKafkaEnvironmentVariables = setOf(
            KAFKA_BOKERS, KAFKA_TRUSTSTORE_PATH, KAFKA_KEYSTORE_PATH, KAFKA_CREDSTORE_PASSWORD
        )
        private const val CLIENT_ID_PREFIX = "producer-k9-punsj-"

        private val logger = LoggerFactory.getLogger(InnsendingClientConfiguration::class.java)
    }
}