package no.nav.k9punsj.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.ContainerStoppingErrorHandler
import java.net.InetAddress
import java.time.Duration
import java.util.*

@Profile("!test")
@Configuration
class KafkaConfig {

    @Value("\${kafka.bootstrap.server}")
    private val bootstrapServers = ""
    @Value("\${kafka.clientId}")
    private val clientId = ""
    @Value("\${systembruker.username}")
    private val username = ""
    @Value("\${systembruker.password}")
    private val password = ""
    @Value("\${javax.net.ssl.trustStore}")
    private val trustStorePath = ""
    @Value("\${javax.net.ssl.trustStorePassword}")
    private val trustStorePassword = ""

    @Bean
    @Qualifier(ON_PREM)
    fun onPremKafkaBaseProperties() = Properties().also { properties ->
        properties[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = requireNotBlank(bootstrapServers) {"Mangler bootstrapServers"}
        properties[CommonClientConfigs.CLIENT_ID_CONFIG] = requireNotBlank(clientId) {"Mangler clientId"}
        properties[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
        properties[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = requireNotBlank(trustStorePath) {"Mangler trustStorePath"}
        properties[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = requireNotBlank(trustStorePassword) {"Mangler trustStorePassword"}
        val jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";"
        val jaasCfg = String.format(jaasTemplate, requireNotBlank(username) {"Mangler username"}, requireNotBlank("password") {"Mangler password"})
        properties[SaslConfigs.SASL_MECHANISM] = "PLAIN"
        properties[SaslConfigs.SASL_JAAS_CONFIG] = jaasCfg
    }

    @Bean
    @Qualifier(AIVEN)
    fun aivenKafkaBaseProperties() = Properties().apply {
        val env = System.getenv()
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, env.getValue(AIVEN_KAFKA_BOKERS))
        put(CommonClientConfigs.CLIENT_ID_CONFIG, "k9-punsj-${InetAddress.getLocalHost().hostName}")
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
        put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
        put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
        put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, env.getValue(AIVEN_KAFKA_TRUSTSTORE_PATH))
        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env.getValue(AIVEN_KAFKA_CREDSTORE_PASSWORD))
        put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, env.getValue(AIVEN_KAFKA_KEYSTORE_PATH))
        put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, env.getValue(AIVEN_KAFKA_CREDSTORE_PASSWORD))
    }

    private fun kafkaListenerContainerFactory(baseProperties: Properties) : KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.containerProperties.apply {
            authorizationExceptionRetryInterval = Duration.ofMillis(RETRY_INTERVAL)
        }
        factory.setErrorHandler(ContainerStoppingErrorHandler())
        factory.consumerFactory = DefaultKafkaConsumerFactory(baseProperties.medConsumerConfig().springMap())
        return factory
    }

    @Bean(ON_PREM_CONTAINER_FACTORY)
    @Qualifier(ON_PREM)
    fun onPremKafkaListenerContainerFactory(
        @Qualifier(ON_PREM) baseProperties: Properties
    ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> =
        kafkaListenerContainerFactory(baseProperties)

    @Bean(AIVEN_CONTAINER_FACTORY)
    @Qualifier(AIVEN)
    fun aivenKafkaListenerContainerFactory(
        @Qualifier(AIVEN) baseProperties: Properties
    ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> =
        kafkaListenerContainerFactory(baseProperties)

    private fun kafkaTemplate(baseProperties: Properties) : KafkaTemplate<String, String> =
        KafkaTemplate(DefaultKafkaProducerFactory(baseProperties.medProducerConfig().springMap()))

    @Bean
    @Qualifier(ON_PREM)
    fun onPremKafkaTemplate(
        @Qualifier(ON_PREM) baseProperties: Properties
    ): KafkaTemplate<String, String> = kafkaTemplate(baseProperties)

    @Bean
    @Qualifier(AIVEN)
    fun aivenKafkaTemplate(
        @Qualifier(AIVEN) baseProperties: Properties
    ): KafkaTemplate<String, String> = kafkaTemplate(baseProperties)

    internal companion object {
        internal const val AIVEN = "aiven"
        internal const val AIVEN_CONTAINER_FACTORY = "aivenKafkaListenerContainerFactory"
        internal const val ON_PREM = "onPrem"
        internal const val ON_PREM_CONTAINER_FACTORY = "onPremKafkaListenerContainerFactory"

        private const val RETRY_INTERVAL = 1000L

        private fun requireNotBlank(verdi: String, feilmelding:() -> String) = verdi.also {
            require(it.isNotBlank()) { feilmelding() }
        }

        private const val AIVEN_KAFKA_BOKERS = "KAFKA_BROKERS"
        private const val AIVEN_KAFKA_TRUSTSTORE_PATH = "KAFKA_TRUSTSTORE_PATH"
        private const val AIVEN_KAFKA_KEYSTORE_PATH = "KAFKA_KEYSTORE_PATH"
        private const val AIVEN_KAFKA_CREDSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD"

        private fun Properties.medProducerConfig() = also { properties ->
            properties[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            properties[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        }
        private fun Properties.medConsumerConfig() = also { properties ->
            properties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            properties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        }
        private fun Properties.springMap() = toMap().mapKeys { "$it" }
    }
}