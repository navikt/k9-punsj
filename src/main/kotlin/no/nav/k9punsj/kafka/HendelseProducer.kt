package no.nav.k9punsj.kafka

interface HendelseProducer {

    fun send(
        topicName: String,
        data: String,
        key: String
    )

    fun sendMedOnSuccess(
        topicName: String,
        data: String,
        key: String,
        onSuccess: () -> Unit = {}
    )
}
