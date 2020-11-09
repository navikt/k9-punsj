package no.nav.k9.fordel

import no.nav.k9.AktørId
import no.nav.k9.JournalpostId
import no.nav.k9.akjonspunkter.Aksjonspunkt
import no.nav.k9.journalpost.Journalpost
import no.nav.k9.journalpost.JournalpostRepository
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.objectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class HendelseMottaker @Autowired constructor(
        val hendelseProducer: HendelseProducer,
        val journalpostRepository: JournalpostRepository,
) {
    private companion object {
        const val topic = "privat-k9punsj-aksjonspunkthendelse-v1"
    }

    suspend fun prosesser(journalpostId: JournalpostId, aktørId: AktørId?) {
        val uuid = UUID.randomUUID()

        journalpostRepository.opprettJournalpost(Journalpost(uuid, journalpostId, aktørId))

        hendelseProducer.send(topic,
                objectMapper().writeValueAsString(PunsjEventDto(uuid.toString(),
                        journalpostId = journalpostId,
                        eventTid = LocalDateTime.now(),
                        aktørId = aktørId,
                        aksjonspunkter = mutableListOf(Aksjonspunkt.OPPRETTET)
                )),
                uuid.toString())
    }
}
