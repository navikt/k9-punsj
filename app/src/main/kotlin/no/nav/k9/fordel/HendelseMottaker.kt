package no.nav.k9.fordel

import no.nav.k9.JournalpostId
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.objectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class HendelseMottaker @Autowired constructor(
        val hendelseProducer: HendelseProducer
){
    private companion object {
        const val topic = "privat-k9punsj-aksjonspunkthendelse"
    }
    fun prosesser(journalpostId: JournalpostId) {
        hendelseProducer.send(topic,  objectMapper().writeValueAsString(K9LosOppgaveDto(journalpostId)), UUID.randomUUID().toString())
    }
}