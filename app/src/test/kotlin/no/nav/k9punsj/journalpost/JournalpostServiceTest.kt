package no.nav.k9punsj.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class JournalpostServiceTest {

    @MockK
    private lateinit var safGateway: SafGateway

    @MockK(relaxUnitFun = true)
    private lateinit var journalpostRepository: JournalpostRepository

    @MockK
    private lateinit var dokarkivGateway: DokarkivGateway

    @MockK
    private lateinit var objectMapper: ObjectMapper

    @InjectMockKs
    private lateinit var journalpostService: JournalpostService

    @Test
    internal fun `forvent kun at journalpost blir satt til ferdig`(): Unit = runBlocking {
        val (status) = journalpostService.settTilFerdig(
            journalpostId = "123",
            ferdigstillJournalpost = false
        )
        coVerify(exactly = 1) { journalpostRepository.ferdig(any()) }
        assertThat(status.is2xxSuccessful)
    }

    @Test
    internal fun `forvent feil dersom journalpost skal ferdigsilles, men enhet mangler`(): Unit = runBlocking {
        val forventetFeil = assertThrows<IllegalArgumentException> {
            journalpostService.settTilFerdig(
                journalpostId = "123",
                ferdigstillJournalpost = true,
                enhet = null
            )
        }

        assertThat(forventetFeil.message)
            .isEqualTo("Enhet kan ikke være null dersom journalpost skal ferdigstilles.")
    }

    @Test
    internal fun `forvent feil dersom journalpost skal ferdigsilles, men sak mangler`(): Unit = runBlocking {
        val forventetFeil = assertThrows<IllegalArgumentException> {
            journalpostService.settTilFerdig(
                journalpostId = "123",
                ferdigstillJournalpost = true,
                enhet = "9999",
                sak = null
            )
        }

        assertThat(forventetFeil.message)
            .isEqualTo("Sak kan ikke være null dersom journalpost skal ferdigstilles.")
    }
}
