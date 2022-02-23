package no.nav.k9punsj.jobber

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.metrikker.Metrikk
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.MetricUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import java.util.*

internal class MetrikkJobbTest {

    private lateinit var metrikkJobb: MetrikkJobb

    private lateinit var metricsEndpoint: MetricsEndpoint

    @BeforeEach
    internal fun setUp() {
        DatabaseUtil.cleanDB()
        val simpleMeterRegistry = SimpleMeterRegistry()
        metrikkJobb = MetrikkJobb(DatabaseUtil.journalpostMetrikkRepository(), simpleMeterRegistry)
        metricsEndpoint = MetricsEndpoint(simpleMeterRegistry)
    }

    @AfterEach
    internal fun tearDown() {
        DatabaseUtil.cleanDB()
    }

    @Test
    fun sjekk_ferdig_behandlede_journalposter(): Unit = runBlocking {
        val journalpostRepo = DatabaseUtil.getJournalpostRepo()
        val dummyAktørId = IdGenerator.nesteId()
        val journalpost = Journalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            type = PunsjInnsendingType.PAPIRSØKNAD.kode
        )

        journalpostRepo.lagre(journalpost) { journalpost }
        journalpostRepo.ferdig(journalpost.journalpostId)
        metrikkJobb.oppdaterMetrikkMåling()

        MetricUtils.assertGuage(metricsEndpoint, Metrikk.ANTALL_FERDIG_BEHANDLEDE_JOURNALPOSTER.navn, 1.0)

    }

    @Test
    fun sjekk_uferdig_behandlede_journalposter(): Unit = runBlocking {
        val journalpostRepo = DatabaseUtil.getJournalpostRepo()
        val dummyAktørId = IdGenerator.nesteId()
        val journalpost = Journalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            type = PunsjInnsendingType.PAPIRSØKNAD.kode
        )

        journalpostRepo.lagre(journalpost) { journalpost }
        metrikkJobb.oppdaterMetrikkMåling()

        MetricUtils.assertGuage(metricsEndpoint, Metrikk.ANTALL_UFERDIGE_BEHANDLEDE_JOURNALPOSTER.navn, 1.0)

    }
}
