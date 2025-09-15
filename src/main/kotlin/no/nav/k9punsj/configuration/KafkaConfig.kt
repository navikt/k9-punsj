package no.nav.k9punsj.configuration

import no.nav.k9punsj.StandardProfil
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import java.net.InetAddress
import java.time.Duration

@Configuration
@StandardProfil
class KafkaConfig(
    @Value("\${kafka.override_truststore_password:}") private val overrideTruststorePassword: String?
) {

    @Bean
    fun kafkaBaseProperties(): Map<String, Any> {
        val env = System.getenv()

        val truststorePasswordKey: String = if (overrideTruststorePassword.isNullOrEmpty())
            CREDSTORE_PASSWORD
        else OVERRIDE_TRUSTSTORE_PASSWORD

        return mapOf(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to env.getValue(KAFKA_BOKERS),
            CommonClientConfigs.CLIENT_ID_CONFIG to "k9-punsj-${InetAddress.getLocalHost().hostName}",
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to (env["KAFKA_SECURITY_PROTOCOL"] ?: SecurityProtocol.SSL.name),
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "jks",
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to env.getValue(KAFKA_TRUSTSTORE_PATH),
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to env.getValue(KAFKA_KEYSTORE_PATH),
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to env.getValue(truststorePasswordKey),
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to env.getValue(CREDSTORE_PASSWORD)
        ) + env["KAFKA_SASL_MECHANISM"]?.let { mapOf("sasl.mechanism" to it) }.orEmpty() + env["KAFKA_SASL_JAAS_CONFIG"]?.let { mapOf("sasl.jaas.config" to it) }.orEmpty()
    }

    private fun byggKafkaConsumerFactory(baseProperties: Map<String, Any>): ConsumerFactory<String, String> =
        DefaultKafkaConsumerFactory(baseProperties.medConsumerConfig())

    private fun byggKafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.containerProperties.setAuthExceptionRetryInterval(Duration.ofMillis(RETRY_INTERVAL))
        factory.setCommonErrorHandler(CommonContainerStoppingErrorHandler())
        factory.consumerFactory = consumerFactory
        return factory
    }

    private fun byggKafkaTemplate(baseProperties: Map<String, Any>): KafkaTemplate<String, String> =
        KafkaTemplate(DefaultKafkaProducerFactory(baseProperties.medProducerConfig()))

    @Bean
    fun kafkaConsumerFactory(
        baseProperties: Map<String, Any>
    ) = byggKafkaConsumerFactory(baseProperties)

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>
    ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> =
        byggKafkaListenerContainerFactory(consumerFactory)

    @Bean
    fun kafkaTemplate(
        baseProperties: Map<String, Any>
    ): KafkaTemplate<String, String> = byggKafkaTemplate(baseProperties)

    internal companion object {

        private const val RETRY_INTERVAL = 1000L

        private const val KAFKA_BOKERS = "KAFKA_BROKERS"
        private const val KAFKA_TRUSTSTORE_PATH = "KAFKA_TRUSTSTORE_PATH"
        private const val KAFKA_KEYSTORE_PATH = "KAFKA_KEYSTORE_PATH"
        private const val CREDSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD"
        private const val OVERRIDE_TRUSTSTORE_PASSWORD = "KAFKA_OVERRIDE_TRUSTSTORE_PASSWORD"

        private fun Map<String, Any>.medProducerConfig() = toMutableMap().also {
            it[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            it[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        }

        private fun Map<String, Any>.medConsumerConfig() = toMutableMap().also {
            it[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        }
    }
}
