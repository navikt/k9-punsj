package no.nav.k9.fordel

import de.huxhorn.sulky.ulid.ULID
import no.nav.k9.JournalpostId
import no.nav.k9.NorskIdent
import no.nav.k9.journalpost.JournalpostService
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.mappe.MappeRepository
import no.nav.k9.objectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class HendelseMottaker @Autowired constructor(
        val hendelseProducer: HendelseProducer,
        val journalpostService: JournalpostService,
        val mappeRepository: MappeRepository
) {
    private companion object {
        const val topic = "privat-k9punsj-aksjonspunkthendelse"
    }

    suspend fun prosesser(journalpostId: JournalpostId, norskIdent :NorskIdent) {
        val ulid = ULID().nextULID()
        
        // bytt ut med servicebrukertoken i stedet for on behalf
        // val hentDokument = journalpostService.hentJournalpostInfo(journalpostId)
        // val norskIdent = hentDokument!!.norskIdent

        hendelseProducer.send(topic,
                objectMapper().writeValueAsString(PunsjEventDto(ulid,
                        journalpostId = journalpostId,
                        eventTid = LocalDateTime.now(),
                        aktørId = norskIdent
                )),
                ulid)
    }
}