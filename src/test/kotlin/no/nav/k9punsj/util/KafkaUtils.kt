package no.nav.k9punsj.util

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils

fun <K, V> EmbeddedKafkaBroker.opprettKafkaStringConsumer(groupId: String, topics: List<String>): Consumer<K, V> {
    val consumerProps = KafkaTestUtils.consumerProps(groupId, "true", this)
    consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    consumerProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 100_000

    val consumer = DefaultKafkaConsumerFactory<K, V>(HashMap(consumerProps)).createConsumer()
    consumer.subscribe(topics)
    return consumer
}
