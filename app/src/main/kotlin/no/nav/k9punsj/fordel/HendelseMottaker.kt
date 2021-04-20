package no.nav.k9punsj.fordel

import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.JournalpostId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

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

        hendelseProducer.send(
            topic,
            objectMapper().writeValueAsString(
                PunsjEventDto(
                    uuid.toString(),
                    journalpostId = journalpostId,
                    eventTid = LocalDateTime.now(),
                    aktørId = aktørId,
                    aksjonspunktKoderMedStatusListe = mutableMapOf(AksjonspunktKode.PUNSJ.kode to AksjonspunktStatus.OPPRETTET.kode)
                )
            ),
            uuid.toString()
        )
    }
}
