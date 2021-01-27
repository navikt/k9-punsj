package no.nav.k9punsj.kafka

interface HendelseProducer {

    fun send(topicName: String, data: String, key: String)
}
