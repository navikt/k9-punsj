package no.nav.k9.kafka

interface HendelseProducer {

    fun send(topicName: String, søknadString: String, søknadId: String)
}
