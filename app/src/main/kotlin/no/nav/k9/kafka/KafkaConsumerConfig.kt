package no.nav.k9.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import java.util.*

@Profile("!test")
@Configuration
internal class KafkaConsumerConfig {

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
    
    @Bean
    fun consumerConfigs(): Map<String, Any?> {

        val props: MutableMap<String, Any> = HashMap()
        props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[CommonClientConfigs.CLIENT_ID_CONFIG] = clientId
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_DOC] = StringSerializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_DOC] = StringSerializer::class.java

        setSecurity(username, props)
        setUsernameAndPassword(username, password, props)
        
        return props
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
    
    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        return DefaultKafkaConsumerFactory(consumerConfigs())
    }

    @Bean
    fun kafkaListenerContainerFactory(): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()
        return factory
    }
}