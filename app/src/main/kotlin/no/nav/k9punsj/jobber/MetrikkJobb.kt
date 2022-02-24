package no.nav.k9punsj.jobber

import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.journalpost.JournalpostMetrikkRepository
import no.nav.k9punsj.metrikker.Metrikk
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MetrikkJobb(
    val journalpostMetrikkRepo: JournalpostMetrikkRepository,
    val meterRegistry: MeterRegistry
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(MetrikkJobb::class.java)
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun oppdaterMetrikkMåling() = runBlocking {
        logger.info("Kjører metrikk jobb: {}", LocalDateTime.now())
        val antallFerdigBehandledeJournalposter = journalpostMetrikkRepo.hentAntallFerdigBehandledeJournalposter(true)
        meterRegistry.gauge(Metrikk.ANTALL_FERDIG_BEHANDLEDE_JOURNALPOSTER.navn, antallFerdigBehandledeJournalposter)

        val antallUferdigBehandledeJournalposter = journalpostMetrikkRepo.hentAntallFerdigBehandledeJournalposter(false)
        meterRegistry.gauge(Metrikk.ANTALL_UFERDIGE_BEHANDLEDE_JOURNALPOSTER.navn, antallUferdigBehandledeJournalposter)

        // TODO: 23/02/2022 antall antall punsj innsendingstyper
    }
}
