package no.nav.k9punsj.fordel

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.fordel.FordelPunsjEventDto.Companion.somFordelPunsjEventDto
import no.nav.k9punsj.kafka.KafkaConfig
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@StandardProfil
class AivenConsumer(
    val hendelseMottaker: HendelseMottaker
) {

    @KafkaListener(
        topics = [PUNSJBAR_JOURNALPOST_TOPIC],
        groupId = "k9-punsj-1",
        properties = ["auto.offset.reset:earliest"],
        containerFactory = KafkaConfig.AIVEN_CONTAINER_FACTORY
    )
    @Throws(IOException::class)
    fun consumePunsjbarJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(PUNSJBAR_JOURNALPOST_TOPIC)) }
    }

    @KafkaListener(
        topics = [FORDEL_JOURNALFORING_TOPIC],
        groupId = "k9-punsj-3",
        properties = ["auto.offset.reset:earliest"],
        containerFactory = KafkaConfig.AIVEN_CONTAINER_FACTORY
    )
    @Throws(IOException::class)
    fun consumeFordelJournalforing(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(FORDEL_JOURNALFORING_TOPIC)) }
    }

    private companion object {
        private const val PUNSJBAR_JOURNALPOST_TOPIC = "k9saksbehandling.punsjbar-journalpost"
        private const val FORDEL_JOURNALFORING_TOPIC = "k9saksbehandling.fordel-journalforing-v1"
    }
}
