package no.nav.k9punsj.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.Sak
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class JournalpostServiceTest: AbstractContainerBaseTest() {

    @MockK
    private lateinit var safGateway: SafGateway

    @MockK(relaxUnitFun = true)
    private lateinit var journalpostRepository: JournalpostRepository

    @MockK
    private lateinit var dokarkivGateway: DokarkivGateway

    @MockK
    private lateinit var objectMapper: ObjectMapper

    @MockK
    private lateinit var k9SakService: K9SakService

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
                enhet = null,
                søkerIdentitetsnummer = "11111111111".somIdentitetsnummer()
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
                sak = null,
                søkerIdentitetsnummer = "11111111111".somIdentitetsnummer()
            )
        }

        assertThat(forventetFeil.message)
            .isEqualTo("Sak kan ikke være null dersom journalpost skal ferdigstilles.")
    }

    @Test
    internal fun `forvent feil dersom journalpost skal ferdigsilles, men søkerIdentitetsnummer mangler`(): Unit = runBlocking {
        val forventetFeil = assertThrows<IllegalArgumentException> {
            journalpostService.settTilFerdig(
                journalpostId = "123",
                ferdigstillJournalpost = true,
                enhet = "9999",
                sak = Sak(sakstype = Sak.SaksType.GENERELL_SAK),
                søkerIdentitetsnummer = null
            )
        }

        assertThat(forventetFeil.message)
            .isEqualTo("SøkerIdentitetsnummer kan ikke være null dersom journalpost skal ferdigstilles.")
    }
}
