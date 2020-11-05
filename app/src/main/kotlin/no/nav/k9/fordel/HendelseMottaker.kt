package no.nav.k9.fordel

import no.nav.k9.AktørId
import no.nav.k9.JournalpostId
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.objectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class HendelseMottaker @Autowired constructor(
        val hendelseProducer: HendelseProducer
) {
    private companion object {
        const val topic = "privat-k9punsj-aksjonspunkthendelse-v1"
    }

    suspend fun prosesser(journalpostId: JournalpostId, aktørId: AktørId?) {
        val uuid = UUID.randomUUID()

        hendelseProducer.send(topic,
                objectMapper().writeValueAsString(PunsjEventDto(uuid.toString(),
                        journalpostId = journalpostId,
                        eventTid = LocalDateTime.now(),
                        aktørId = aktørId
                )),
                uuid.toString())
    }
}
