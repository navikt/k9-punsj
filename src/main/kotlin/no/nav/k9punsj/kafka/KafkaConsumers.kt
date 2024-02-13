package no.nav.k9punsj.kafka

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.fordel.FordelPunsjEventDto.Companion.somFordelPunsjEventDto
import no.nav.k9punsj.fordel.HendelseMottaker
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@StandardProfil
class KafkaConsumers(
    val hendelseMottaker: HendelseMottaker,
    @Value("\${no.nav.kafka.k9_punsjbolle.topic}") private val meldingerFraPunsjbolleTopic: String,
    @Value("\${no.nav.kafka.k9_fordel.topic}") private val meldingerFraFordelTopic: String,
) {

    @KafkaListener(
        topics = [PUNSJBOLLE_TOPIC],
        groupId = "k9-punsj-1",
        properties = ["auto.offset.reset:earliest"]
    )
    @Throws(IOException::class)
    fun consumePunsjbarJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(meldingerFraPunsjbolleTopic)) }
    }

    @KafkaListener(
        topics = [FORDEL_TOPIC],
        groupId = "k9-punsj-1",
        properties = ["auto.offset.reset:earliest"]
    )
    @Throws(IOException::class)
    fun consumeFordelJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(meldingerFraFordelTopic)) }
    }

    private companion object {
        private const val PUNSJBOLLE_TOPIC = "k9saksbehandling.punsjbar-journalpost"
        private const val FORDEL_TOPIC = "k9saksbehandling.fordel-journalforing"
    }
}
