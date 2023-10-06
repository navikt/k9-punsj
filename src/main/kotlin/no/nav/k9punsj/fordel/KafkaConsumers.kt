package no.nav.k9punsj.fordel

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.fordel.FordelPunsjEventDto.Companion.somFordelPunsjEventDto
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@StandardProfil
class KafkaConsumers(
    val hendelseMottaker: HendelseMottaker
) {

    @KafkaListener(
        topics = [PUNSJBOLLE_TOPIC],
        groupId = "k9-punsj-1",
        properties = ["auto.offset.reset:earliest"]
    )
    @Throws(IOException::class)
    fun consumePunsjbarJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(PUNSJBOLLE_TOPIC)) }
    }

    @KafkaListener(
        topics = [FORDEL_TOPIC],
        groupId = "k9-punsj-1",
        properties = ["auto.offset.reset:earliest"]
    )
    @Throws(IOException::class)
    fun consumeFordelJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(FORDEL_TOPIC)) }
    }

    private companion object {
        private const val PUNSJBOLLE_TOPIC = "k9saksbehandling.punsjbar-journalpost"
        private const val FORDEL_TOPIC = "k9saksbehandling.fordel-journalforing"
    }
}
