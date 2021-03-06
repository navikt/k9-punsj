package no.nav.k9punsj.fordel

import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
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

    suspend fun prosesser(fordelPunsjEventDto: FordelPunsjEventDto) {
        val journalpostId = fordelPunsjEventDto.journalpostId
        val aktørId = fordelPunsjEventDto.aktørId
        val fantIkke = journalpostRepository.fantIkke(journalpostId)

        if (fantIkke) {
            val uuid = UUID.randomUUID()
            val journalpost = Journalpost(
                uuid = uuid,
                journalpostId = journalpostId,
                aktørId = aktørId,
                type = if(fordelPunsjEventDto.type!=null) PunsjInnsendingType.fraKode(fordelPunsjEventDto.type).kode else null,
                opprinneligJournalpost = if(fordelPunsjEventDto.opprinneligJournalpost != null) Journalpost.OpprinneligJournalpost(
                    fordelPunsjEventDto.opprinneligJournalpost.journalpostId) else null
            )
            journalpostRepository.opprettJournalpost(journalpost)
            aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
                journalpost = journalpost,
                aksjonspunkt = Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET),
                type = if(fordelPunsjEventDto.type!=null) PunsjInnsendingType.fraKode(fordelPunsjEventDto.type).kode else null,
                ytelse = fordelPunsjEventDto.ytelse)
        } else {
            log.info("Journalposten($journalpostId) kjenner punsj fra før, blir ikke laget ny oppgave")
        }
    }
}
