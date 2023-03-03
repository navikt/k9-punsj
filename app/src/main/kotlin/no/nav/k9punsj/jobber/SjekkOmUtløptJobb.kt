package no.nav.k9punsj.jobber

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.akjonspunkter.AksjonspunktEntitet
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.fordel.PunsjEventDto
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.utils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class SjekkOmUtløptJobb @Autowired constructor(
    val aksjonspunktRepository: AksjonspunktRepository,
    val hendelseProducer: HendelseProducer,
    val journalpostRepository: JournalpostRepository,
    @Value("\${no.nav.kafka.k9_los.topic}") private val k9losAksjonspunkthendelseTopic: String
) {

    private val logger = LoggerFactory.getLogger(SjekkOmUtløptJobb::class.java)

    // kjører klokken 04:00
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
                        aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
                    )
                )
            }.toList()

            for (aksjonspunkt in nyeAksjonspunkter) {
                val journalpost = journalpostRepository.hent(aksjonspunkt.journalpostId)
                sendTilLos(journalpost, aksjonspunkt)
            }
            logger.info("Jobben(SjekkOmUtløptJobb) er kjørt ferdig, fant " + aksjonspunkter.size + " aksjonspunkt(er) som har løpt ut")
        }
    }

    private fun sendTilLos(punsjJournalpost: PunsjJournalpost, aksjonspunkt: AksjonspunktEntitet) {
        val punsjEventJson = objectMapper().writeValueAsString(
            PunsjEventDto(
                eksternId = punsjJournalpost.uuid.toString(),
                journalpostId = punsjJournalpost.journalpostId,
                eventTid = LocalDateTime.now(),
                aktørId = punsjJournalpost.aktørId,
                aksjonspunktKoderMedStatusListe = mutableMapOf(
                    aksjonspunkt.aksjonspunktKode.kode to AksjonspunktStatus.UTFØRT.kode,
                    AksjonspunktKode.PUNSJ_HAR_UTLØPT.kode to AksjonspunktStatus.OPPRETTET.kode
                )
            )
        )
        hendelseProducer.send(
            topicName = k9losAksjonspunkthendelseTopic,
            data = punsjEventJson,
            key = punsjJournalpost.uuid.toString()
        )
    }
}
