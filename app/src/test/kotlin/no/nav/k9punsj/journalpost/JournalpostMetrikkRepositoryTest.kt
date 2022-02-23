package no.nav.k9punsj.journalpost

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@ExtendWith(SpringExtension::class, MockKExtension::class)
@TestPropertySource(locations = ["classpath:application.yml"])
internal class JournalpostMetrikkRepositoryTest {

    @BeforeEach
    internal fun setUp() {
        DatabaseUtil.cleanDB()
    }

    @AfterEach
    internal fun tearDown() {
        DatabaseUtil.cleanDB()
    }

    @Test
    fun hentAntallFerdigBehandledeJournalposter(): Unit = runBlocking {
        val journalpostRepo = DatabaseUtil.getJournalpostRepo()
        val journalpostMetrikkRepository = DatabaseUtil.journalpostMetrikkRepository()

        val dummyAktørId = IdGenerator.nesteId()
        val journalpost = Journalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            type = PunsjInnsendingType.PAPIRSØKNAD.kode
        )

        journalpostRepo.lagre(journalpost) { journalpost }
        journalpostRepo.ferdig(journalpost.journalpostId)

        val antallFerdigBehandledeJournalposter =
            journalpostMetrikkRepository.hentAntallFerdigBehandledeJournalposter(true)
        assertThat(antallFerdigBehandledeJournalposter).isEqualTo(1)
    }
}
