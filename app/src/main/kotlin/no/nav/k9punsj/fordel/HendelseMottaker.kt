package no.nav.k9punsj.fordel

import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.rest.web.JournalpostId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HendelseMottaker @Autowired constructor(
    val journalpostRepository: JournalpostRepository,
    val aksjonspunktService: AksjonspunktService
) {
    private companion object {
        private val log: Logger = LoggerFactory.getLogger(HendelseMottaker::class.java)
    }

    suspend fun prosesser(journalpostId: JournalpostId, aktørId: AktørId?) {
        val fantIkke = journalpostRepository.fantIkke(journalpostId)

        if (fantIkke) {
            val uuid = UUID.randomUUID()
            val journalpost = Journalpost(uuid, journalpostId, aktørId)
            journalpostRepository.opprettJournalpost(journalpost)
            aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(journalpost, Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET))
        } else {
            log.info("Journalposten($journalpostId) kjenner punsj fra før, blir ikke laget ny oppgave")
        }
    }
}
