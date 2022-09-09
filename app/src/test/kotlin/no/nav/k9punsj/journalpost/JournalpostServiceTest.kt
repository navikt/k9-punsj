package no.nav.k9punsj.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class JournalpostServiceTest {

    @Mock
    private lateinit var safGateway: SafGateway

    @Mock
    private lateinit var journalpostRepository: JournalpostRepository

    @Mock
    private lateinit var dokarkivGateway: DokarkivGateway

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @InjectMocks
    private lateinit var journalpostService: JournalpostService

    @BeforeEach
    internal fun setUp() {
        println("Starting up...")
    }

    @Test
    internal fun `forvent kun at journalpost blir satt til ferdig`(): Unit = runBlocking {
        val (status) = journalpostService.settTilFerdig(
            journalpostId = "123",
            ferdigstillJournalpost = false
        )
        assertThat(status.is2xxSuccessful)
    }
}
