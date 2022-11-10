package no.nav.k9punsj.kafka

interface HendelseProducerOnprem {

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
