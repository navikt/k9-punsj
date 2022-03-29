package no.nav.k9punsj.jobber

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.SafGateway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class FinnJournalposterMedFeilBrevkodeOchLukk @Autowired constructor(
    val aksjonspunktService: AksjonspunktService,
    val safGateway: SafGateway,
    val journalpostRepository: JournalpostRepository,
) {

    private val logger = LoggerFactory.getLogger(FinnJournalposterMedFeilBrevkodeOchLukk::class.java)
    private val FEIL_BREVKODE = "CRM_MELDINGSKJEDE"

    @Scheduled(cron = "0 30 8 * * *")
    fun finnJournalposterMedBrevkodeSomIkkeSkallBehandlesOchLukk() {
        logger.info("Kjører scheduled job FinnJournalposterMedFeilBrevkodeOchLukk")
        runBlocking {
            val åpneBehandlinger = journalpostRepository.finnAlleÅpneJournalposter()
            logger.info("Fann ${åpneBehandlinger.size} åpne behandlinger")


            val testerMedEnLitenListe = listOf(åpneBehandlinger[0], åpneBehandlinger[1], åpneBehandlinger[2])

            val åpneBehandlingerMedBrevkodeCRM = safGateway.hentJournalposter(
                journalpostIder = testerMedEnLitenListe,
                erSystemKall = true
            )
                .filterNotNull()
                .filter { journalpost -> journalpost.dokumenter.any { it.brevkode == FEIL_BREVKODE } }
                .map { it.journalpostId }

            if (åpneBehandlingerMedBrevkodeCRM.isEmpty()) {
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
