package no.nav.k9punsj.jobber

import kotlinx.coroutines.runBlocking
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9punsj.akjonspunkter.AksjonspunktEntitet
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.dokarkiv.SafDtos
import no.nav.k9punsj.fordel.PunsjEventDto
import no.nav.k9punsj.journalpost.JournalpostId
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.SafGateway
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.log

@Service
class FinnJournalposterMedFeilBrevkodeOchLukk @Autowired constructor(
    val aksjonspunktService: AksjonspunktService,
    val safGateway: SafGateway,
    val hendelseProducer: HendelseProducer,
    val journalpostRepository: JournalpostRepository,
) {

    private val logger = LoggerFactory.getLogger(FinnJournalposterMedFeilBrevkodeOchLukk::class.java)
    private val FEIL_BREVKODE = "CRM_MELDINGSKJEDE"

    //kjører klokken 04:00
    @Scheduled(cron = "0 0 4 * * *")
    fun finnJournalposterMedBrevkodeSomIkkeSkallBehandlesOchLukk() {
        logger.info("Kjører scheduled job FinnJournalposterMedFeilBrevkodeOchLukk")
        runBlocking {
            val åpneBehandlinger = journalpostRepository.finnAlleÅpneJournalposter()
            logger.info("Fann ${åpneBehandlinger.size} åpne behandlinger")
            val åpneBehandlingerMedBrevkodeCRM = safGateway.hentJournalposter(åpneBehandlinger)
                .filterNotNull()
                .filter { journalpost -> journalpost.dokumenter.any { it.brevkode == FEIL_BREVKODE } }
                .map { it.journalpostId }

            if(åpneBehandlingerMedBrevkodeCRM.isEmpty()) {
                logger.info("Fann inga åpne behandlinger som har brevkode $FEIL_BREVKODE i SAF")
                return@runBlocking
            }

            logger.info("Åpne behandlinger med brevkode CRM_MELDINGSKJEDE som lukkes i punsj och los: ${åpneBehandlingerMedBrevkodeCRM.size}")
            journalpostRepository.settAlleTilFerdigBehandlet(åpneBehandlingerMedBrevkodeCRM)
            aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(åpneBehandlingerMedBrevkodeCRM, false, null)
        }
        logger.info("FinnJournalposterMedFeilBrevkodeOchLukk ferdig")
    }

}
