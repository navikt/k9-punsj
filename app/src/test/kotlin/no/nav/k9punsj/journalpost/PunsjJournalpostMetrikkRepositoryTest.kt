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
import java.util.stream.IntStream

@ExtendWith(SpringExtension::class, MockKExtension::class)
@TestPropertySource(locations = ["classpath:application.yml"])
internal class PunsjJournalpostMetrikkRepositoryTest {

    private val journalpostRepo = DatabaseUtil.getJournalpostRepo()
    private val journalpostMetrikkRepository = DatabaseUtil.journalpostMetrikkRepository()

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
        val journalpost = opprettJournalpost(IdGenerator.nesteId(), PunsjInnsendingType.PAPIRSØKNAD)

        journalpostRepo.lagre(journalpost) { journalpost }
        journalpostRepo.ferdig(journalpost.journalpostId)

        val antallFerdigBehandledeJournalposter =
            journalpostMetrikkRepository.hentAntallFerdigBehandledeJournalposter(true)
        assertThat(antallFerdigBehandledeJournalposter).isEqualTo(1)
    }

    @Test
    fun hentAntallPunsjInnsendingstyper(): Unit = runBlocking {
        genererJournalposter(antall = 9, type = PunsjInnsendingType.PAPIRSØKNAD)
        genererJournalposter(antall = 8, type = PunsjInnsendingType.DIGITAL_ETTERSENDELSE)
        genererJournalposter(antall = 7, type = PunsjInnsendingType.PAPIRETTERSENDELSE)
        genererJournalposter(antall = 6, type = PunsjInnsendingType.KOPI)
        genererJournalposter(antall = 5, type = PunsjInnsendingType.INNLOGGET_CHAT)
        genererJournalposter(antall = 4, type = PunsjInnsendingType.INNTEKTSMELDING_UTGÅTT)
        genererJournalposter(antall = 3, type = PunsjInnsendingType.PAPIRINNTEKTSOPPLYSNINGER)
        genererJournalposter(antall = 2, type = PunsjInnsendingType.SKRIV_TIL_OSS_SPØRMSÅL)
        genererJournalposter(antall = 1, type = PunsjInnsendingType.SKRIV_TIL_OSS_SVAR)

        val antallTyper = journalpostMetrikkRepository.hentAntallJournalposttyper()
        assertThat(antallTyper).containsExactlyInAnyOrder(
            Pair(9, PunsjInnsendingType.PAPIRSØKNAD),
            Pair(8, PunsjInnsendingType.DIGITAL_ETTERSENDELSE),
            Pair(7, PunsjInnsendingType.PAPIRETTERSENDELSE),
            Pair(6, PunsjInnsendingType.KOPI),
            Pair(5, PunsjInnsendingType.INNLOGGET_CHAT),
            Pair(4, PunsjInnsendingType.INNTEKTSMELDING_UTGÅTT),
            Pair(3, PunsjInnsendingType.PAPIRINNTEKTSOPPLYSNINGER),
            Pair(2, PunsjInnsendingType.SKRIV_TIL_OSS_SPØRMSÅL),
            Pair(1, PunsjInnsendingType.SKRIV_TIL_OSS_SVAR)
        )
    }

    private suspend fun opprettJournalpost(dummyAktørId: String, type: PunsjInnsendingType): PunsjJournalpost {
        val punsjJournalpost = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            type = type.kode
        )
        journalpostRepo.lagre(punsjJournalpost) { punsjJournalpost }
        return punsjJournalpost
    }

    private suspend fun genererJournalposter(antall: Int, type: PunsjInnsendingType) {
        IntStream.range(0, antall).forEach {
            runBlocking {
                opprettJournalpost(IdGenerator.nesteId(), type)
            }
        }
    }
}
