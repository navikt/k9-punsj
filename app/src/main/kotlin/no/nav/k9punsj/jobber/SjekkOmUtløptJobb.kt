package no.nav.k9punsj.jobber

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.akjonspunkter.AksjonspunktEntitet
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.fordel.PunsjEventDto
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class SjekkOmUtløptJobb @Autowired constructor(
    val aksjonspunktRepository: AksjonspunktRepository,
    val hendelseProducer: HendelseProducer,
    val journalpostRepository: JournalpostRepository,
) {

    private val logger = LoggerFactory.getLogger(SjekkOmUtløptJobb::class.java)

    //kjører klokken 04:00
    @Scheduled(cron = "0 0 4 * * *")
    fun sjekkeOmAksjonspunktHarLøptUt() {
        runBlocking {
            val aksjonspunkter: List<AksjonspunktEntitet> =
                aksjonspunktRepository.hentAksjonspunkterDerFristenHarLøptUt()

            aksjonspunkter.forEach { aksjonspunktEntitet ->
                aksjonspunktRepository.settStatus(aksjonspunktEntitet.aksjonspunktId, AksjonspunktStatus.UTFØRT)
            }

            val nyeAksjonspunkter = aksjonspunkter.map {
                aksjonspunktRepository.opprettAksjonspunkt(
                    AksjonspunktEntitet(
                    aksjonspunktId = UUID.randomUUID().toString(),
                    aksjonspunktKode = AksjonspunktKode.PUNSJ_HAR_UTLØPT,
                    journalpostId = it.journalpostId,
                    aksjonspunktStatus = AksjonspunktStatus.OPPRETTET))
            }.toList()

            for (aksjonspunkt in nyeAksjonspunkter) {
                val journalpost = journalpostRepository.hent(aksjonspunkt.journalpostId)
                sendTilLos(journalpost, aksjonspunkt)
            }
            logger.info("Jobben(SjekkOmUtløptJobb) er kjørt ferdig, fant " + aksjonspunkter.size + " aksjonspunkt(er) som har løpt ut")
        }
    }

    private fun sendTilLos(journalpost: Journalpost, aksjonspunkt: AksjonspunktEntitet) {
        hendelseProducer.send(
            Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
            objectMapper().writeValueAsString(
                PunsjEventDto(
                    journalpost.uuid.toString(),
                    journalpostId = journalpost.journalpostId,
                    eventTid = LocalDateTime.now(),
                    aktørId = journalpost.aktørId,
                    aksjonspunktKoderMedStatusListe = mutableMapOf(aksjonspunkt.aksjonspunktKode.kode to aksjonspunkt.aksjonspunktStatus.kode)
                )
            ),
            journalpost.uuid.toString()
        )
    }
}
