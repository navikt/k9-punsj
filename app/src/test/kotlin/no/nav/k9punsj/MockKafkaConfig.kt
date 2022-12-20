package no.nav.k9punsj

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
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
import java.time.Duration

@Configuration
@LokalProfil
class MockKafkaConfig {

    @Bean
    @Qualifier(AIVEN)
    fun aivenKafkaBaseProperties(): Map<String, Any> {
        val env = System.getenv()

        return mapOf(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to env.getOrDefault(AIVEN_KAFKA_BOKERS, "localhost:9092"),
            CommonClientConfigs.CLIENT_ID_CONFIG to "k9-punsj-lokalt",
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
    @Qualifier(AIVEN)
    fun aivenKafkaConsumerFactory(
        @Qualifier(AIVEN) baseProperties: Map<String, Any>
    ) = kafkaConsumerFactory(baseProperties)

    @Bean(AIVEN_CONTAINER_FACTORY)
    @Qualifier(AIVEN)
    fun aivenKafkaListenerContainerFactory(
        @Qualifier(AIVEN) consumerFactory: ConsumerFactory<String, String>
    ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> =
        kafkaListenerContainerFactory(consumerFactory)

    @Bean
    @Qualifier(AIVEN)
    fun aivenKafkaTemplate(
        @Qualifier(AIVEN) baseProperties: Map<String, Any>
    ): KafkaTemplate<String, String> = kafkaTemplate(baseProperties)

    internal companion object {

        internal const val AIVEN = "aiven"
        internal const val AIVEN_CONTAINER_FACTORY = "aivenKafkaListenerContainerFactory"

        private const val RETRY_INTERVAL = 1000L

        private const val AIVEN_KAFKA_BOKERS = "KAFKA_BROKERS"

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
