package no.nav.k9punsj.fordel

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.fordel.FordelPunsjEventDto.Companion.somFordelPunsjEventDto
import no.nav.k9punsj.configuration.KafkaConfig
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@StandardProfil
class KafkaConsumers(
    val hendelseMottaker: HendelseMottaker
) {

    @KafkaListener(
        topics = [PUNSJBOLLE_AIVEN_TOPIC],
        groupId = "k9-punsj-1",
        properties = ["auto.offset.reset:earliest"],
        containerFactory = KafkaConfig.AIVEN_CONTAINER_FACTORY
    )
    @Throws(IOException::class)
    fun consumeAivenPunsjbarJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(PUNSJBOLLE_AIVEN_TOPIC)) }
    }

    @KafkaListener(
        topics = [FORDEL_TOPIC],
        groupId = "k9-punsj-1",
        properties = ["auto.offset.reset:earliest"],
        containerFactory = KafkaConfig.AIVEN_CONTAINER_FACTORY
    )
    @Throws(IOException::class)
    fun consumeAivenFordelJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(FORDEL_TOPIC)) }
    }

    private companion object {
        private const val PUNSJBOLLE_AIVEN_TOPIC = "k9saksbehandling.punsjbar-journalpost"
        private const val FORDEL_TOPIC = "k9saksbehandling.fordel-journalforing"
    }
}
