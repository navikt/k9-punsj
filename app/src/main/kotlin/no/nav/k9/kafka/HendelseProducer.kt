package no.nav.k9.kafka

interface HendelseProducer {

    fun send(topicName: String, data: String, key: String)
}
