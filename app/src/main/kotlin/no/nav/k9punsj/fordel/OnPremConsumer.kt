package no.nav.k9punsj.fordel

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.IkkeTestProfil
import no.nav.k9punsj.fordel.FordelPunsjEventDto.Companion.somFordelPunsjEventDto
import no.nav.k9punsj.kafka.KafkaConfig
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@IkkeTestProfil
class OnPremConsumer(
    val hendelseMottaker: HendelseMottaker
) {

    @KafkaListener(
        topics = [ON_PREM_TOPIC],
        groupId = "k9-punsj-3",
        properties = ["auto.offset.reset:earliest"],
        containerFactory = KafkaConfig.ON_PREM_CONTAINER_FACTORY
    )
    @Throws(IOException::class)
    fun consumeOnPremPunsjbarJournalpost(message: String) {
        runBlocking { hendelseMottaker.prosesser(message.somFordelPunsjEventDto(ON_PREM_TOPIC)) }
    }

    private companion object {
        private const val ON_PREM_TOPIC = "privat-k9-fordel-journalforing-v1"
    }
}
