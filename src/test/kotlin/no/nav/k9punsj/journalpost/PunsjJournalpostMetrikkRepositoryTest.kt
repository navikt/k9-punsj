package no.nav.k9punsj.journalpost

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.fordel.K9FordelType
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.metrikker.JournalpostMetrikkRepository
import no.nav.k9punsj.util.IdGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import java.util.stream.IntStream

internal class PunsjJournalpostMetrikkRepositoryTest: AbstractContainerBaseTest() {

    @Autowired
    lateinit var journalpostRepo: JournalpostRepository

    @Autowired
    lateinit var journalpostMetrikkRepository: JournalpostMetrikkRepository

    @BeforeEach
    internal fun setUp() {
        cleanUpDB()
    }

    @AfterEach
    internal fun tearDown() {
       cleanUpDB()
    }

    @Test
    fun hentAntallFerdigBehandledeJournalposter(): Unit = runBlocking {
        val journalpost = opprettJournalpost(IdGenerator.nesteId(), K9FordelType.PAPIRSØKNAD)

        journalpostRepo.lagre(journalpost) { journalpost }
        journalpostRepo.ferdig(journalpost.journalpostId)

        val antallFerdigBehandledeJournalposter =
            journalpostMetrikkRepository.hentAntallFerdigBehandledeJournalposter(true)
        assertThat(antallFerdigBehandledeJournalposter).isEqualTo(1)
    }

    @Test
    fun hentAntallPunsjInnsendingstyper(): Unit = runBlocking {
        genererJournalposter(antall = 9, type = K9FordelType.PAPIRSØKNAD)
        genererJournalposter(antall = 8, type = K9FordelType.DIGITAL_ETTERSENDELSE)
        genererJournalposter(antall = 7, type = K9FordelType.PAPIRETTERSENDELSE)
        genererJournalposter(antall = 6, type = K9FordelType.KOPI)
        genererJournalposter(antall = 5, type = K9FordelType.INNLOGGET_CHAT)
        genererJournalposter(antall = 4, type = K9FordelType.INNTEKTSMELDING_UTGÅTT)
        genererJournalposter(antall = 3, type = K9FordelType.PAPIRINNTEKTSOPPLYSNINGER)
        genererJournalposter(antall = 2, type = K9FordelType.SKRIV_TIL_OSS_SPØRMSÅL)
        genererJournalposter(antall = 1, type = K9FordelType.SKRIV_TIL_OSS_SVAR)

        val antallTyper = journalpostMetrikkRepository.hentAntallJournalposttyper()
        assertThat(antallTyper).containsExactlyInAnyOrder(
            Pair(9, K9FordelType.PAPIRSØKNAD),
            Pair(8, K9FordelType.DIGITAL_ETTERSENDELSE),
            Pair(7, K9FordelType.PAPIRETTERSENDELSE),
            Pair(6, K9FordelType.KOPI),
            Pair(5, K9FordelType.INNLOGGET_CHAT),
            Pair(4, K9FordelType.INNTEKTSMELDING_UTGÅTT),
            Pair(3, K9FordelType.PAPIRINNTEKTSOPPLYSNINGER),
            Pair(2, K9FordelType.SKRIV_TIL_OSS_SPØRMSÅL),
            Pair(1, K9FordelType.SKRIV_TIL_OSS_SVAR)
        )
    }

    private suspend fun opprettJournalpost(dummyAktørId: String, type: K9FordelType): PunsjJournalpost {
        val punsjJournalpost = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            type = type.kode
        )
        journalpostRepo.lagre(punsjJournalpost) { punsjJournalpost }
        return punsjJournalpost
    }

    private suspend fun genererJournalposter(antall: Int, type: K9FordelType) {
        IntStream.range(0, antall).forEach {
            runBlocking {
                opprettJournalpost(IdGenerator.nesteId(), type)
            }
        }
    }
}
