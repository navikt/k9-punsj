package no.nav.k9punsj.kafka

import no.nav.k9punsj.IkkeLokalProfil
import no.nav.k9punsj.IkkeTestProfil
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
@IkkeTestProfil
class KafkaConfig(
    @Value("\${kafka.bootstrap.server}") private val bootstrapServers: String,
    @Value("\${kafka.clientId}") private val clientId: String,
    @Value("\${systembruker.username}") private val username: String,
    @Value("\${systembruker.password}") private val password: String,
    @Value("\${javax.net.ssl.trustStore}") private val trustStorePath: String,
    @Value("\${javax.net.ssl.trustStorePassword}") private val trustStorePassword: String,
    @Value("\${kafka.override_truststore_password:}") private val overrideTruststorePassword: String?
) {

    @Bean
    @Qualifier(ON_PREM)
    fun onPremKafkaBaseProperties(): Map<String, Any> {
        requireNotBlank(username) { "Mangler username" }
        requireNotBlank(password) { "Mangler password" }
        return mapOf(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to requireNotBlank(bootstrapServers) { "Mangler bootstrapServers" },
            CommonClientConfigs.CLIENT_ID_CONFIG to requireNotBlank(clientId) { "Mangler clientId" },
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to requireNotBlank(trustStorePath) { "Mangler trustStorePath" },
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to requireNotBlank(trustStorePassword) { "Mangler trustStorePassword" },
            SaslConfigs.SASL_MECHANISM to "PLAIN",
            SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${username}\" password=\"${password}\";"
        )
    }

    @Bean
    @Qualifier(AIVEN)
    @IkkeLokalProfil
    fun aivenKafkaBaseProperties(): Map<String, Any> {
        val env = System.getenv()

        val truststorePasswordKey: String = if (overrideTruststorePassword.isNullOrEmpty())
            AIVEN_KAFKA_CREDSTORE_PASSWORD
        else AIVEN_KAFKA_OVERRIDE_TRUSTSTORE_PASSWORD

        return mapOf(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to env.getValue(AIVEN_KAFKA_BOKERS),
            CommonClientConfigs.CLIENT_ID_CONFIG to "k9-punsj-${InetAddress.getLocalHost().hostName}",
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "jks",
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to env.getValue(AIVEN_KAFKA_TRUSTSTORE_PATH),
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to env.getValue(AIVEN_KAFKA_KEYSTORE_PATH),
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to env.getValue(truststorePasswordKey),
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to env.getValue(AIVEN_KAFKA_CREDSTORE_PASSWORD)
        )
    }

    private fun kafkaConsumerFactory(baseProperties: Map<String, Any>): ConsumerFactory<String, String> =
        DefaultKafkaConsumerFactory(baseProperties.medConsumerConfig())

    private fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.containerProperties.setAuthExceptionRetryInterval(Duration.ofMillis(RETRY_INTERVAL))
        factory.setCommonErrorHandler(CommonContainerStoppingErrorHandler())
        factory.consumerFactory = consumerFactory
        return factory
    }

    private fun kafkaTemplate(baseProperties: Map<String, Any>): KafkaTemplate<String, String> =
        KafkaTemplate(DefaultKafkaProducerFactory(baseProperties.medProducerConfig()))

    @Bean
    @Qualifier(ON_PREM)
    fun onPremKafkaConsumerFactory(
        @Qualifier(ON_PREM) baseProperties: Map<String, Any>
    ) = kafkaConsumerFactory(baseProperties)

    @Bean
    @Qualifier(AIVEN)
    @IkkeLokalProfil
    fun aivenKafkaConsumerFactory(
        @Qualifier(AIVEN) baseProperties: Map<String, Any>
    ) = kafkaConsumerFactory(baseProperties)

    @Bean(ON_PREM_CONTAINER_FACTORY)
    @Qualifier(ON_PREM)
    fun onPremKafkaListenerContainerFactory(
        @Qualifier(ON_PREM) consumerFactory: ConsumerFactory<String, String>
    ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> =
        kafkaListenerContainerFactory(consumerFactory)

    @Bean(AIVEN_CONTAINER_FACTORY)
    @Qualifier(AIVEN)
    @IkkeLokalProfil
    fun aivenKafkaListenerContainerFactory(
        @Qualifier(AIVEN) consumerFactory: ConsumerFactory<String, String>
    ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> =
        kafkaListenerContainerFactory(consumerFactory)

    @Bean
    @Qualifier(ON_PREM)
    fun onPremKafkaTemplate(
        @Qualifier(ON_PREM) baseProperties: Map<String, Any>
    ): KafkaTemplate<String, String> = kafkaTemplate(baseProperties)

    @Bean
    @Qualifier(AIVEN)
    @IkkeLokalProfil
    fun aivenKafkaTemplate(
        @Qualifier(AIVEN) baseProperties: Map<String, Any>
    ): KafkaTemplate<String, String> = kafkaTemplate(baseProperties)

    internal companion object {

        internal const val AIVEN = "aiven"
        internal const val AIVEN_CONTAINER_FACTORY = "aivenKafkaListenerContainerFactory"
        internal const val ON_PREM = "onPrem"
        internal const val ON_PREM_CONTAINER_FACTORY = "onPremKafkaListenerContainerFactory"

        private const val RETRY_INTERVAL = 1000L

        private fun requireNotBlank(verdi: String, feilmelding: () -> String) = verdi.also {
            require(it.isNotBlank()) { feilmelding() }
        }

        private const val AIVEN_KAFKA_BOKERS = "KAFKA_BROKERS"
        private const val AIVEN_KAFKA_TRUSTSTORE_PATH = "KAFKA_TRUSTSTORE_PATH"
        private const val AIVEN_KAFKA_KEYSTORE_PATH = "KAFKA_KEYSTORE_PATH"
        private const val AIVEN_KAFKA_CREDSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD"
        private const val AIVEN_KAFKA_OVERRIDE_TRUSTSTORE_PASSWORD = "KAFKA_OVERRIDE_TRUSTSTORE_PASSWORD"

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
