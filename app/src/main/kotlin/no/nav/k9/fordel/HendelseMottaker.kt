package no.nav.k9.fordel

import de.huxhorn.sulky.ulid.ULID
import no.nav.k9.JournalpostId
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.objectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class HendelseMottaker @Autowired constructor(
        val hendelseProducer: HendelseProducer
) {
    private companion object {
        const val topic = "privat-k9punsj-aksjonspunkthendelse"
    }

    fun prosesser(journalpostId: JournalpostId) {
        val ulid = ULID().nextULID()
        hendelseProducer.send(topic,
                objectMapper().writeValueAsString(PunsjEventDto(ulid,
                        journalpostId = journalpostId,
                        eventTid = LocalDateTime.now())),
                ulid)
    }
}