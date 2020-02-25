package no.nav.k9.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class KafkaPropertiesUtil {

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


    fun opprettProperties(): Properties {

        val properties = Properties()
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId)
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

        setSecurity(username, properties)
        setUsernameAndPassword(username, password, properties)

        return properties
    }

    private fun setSecurity(username: String?, properties: Properties) {
        if (username != null && !username.isEmpty()) {
            properties.put("security.protocol", "SASL_SSL")
            properties.put("sasl.mechanism", "PLAIN")
            properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath)
            properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword)
        }
    }

    private fun setUsernameAndPassword(username: String?, password: String?, properties: Properties) {
        if (username != null && !username.isEmpty()
                && password != null && !password.isEmpty()) {
            val jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";"
            val jaasCfg = String.format(jaasTemplate, username, password)
            properties.setProperty("sasl.jaas.config", jaasCfg)
        }
    }

}
