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
        topics = [AIVEN_TOPIC],
        groupId = "k9-punsj-1",
        properties = ["auto.offset.reset:earliest"],
        containerFactory = KafkaConfig.AIVEN_CONTAINER_FACTORY
    )
    @Throws(IOException::class)
    fun consumeAivenPunsjbarJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(AIVEN_TOPIC)) }
    }

    private companion object {
        private const val AIVEN_TOPIC = "k9saksbehandling.punsjbar-journalpost"
    }
}
