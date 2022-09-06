package no.nav.k9punsj.rest.eksternt.punsjbollen

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.integrasjoner.infotrygd.InfotrygdClient
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.LopendeSakDto
import no.nav.k9punsj.ruting.Destinasjon
import no.nav.k9punsj.ruting.RutingGrunnlag
import no.nav.k9punsj.ruting.RutingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RutingServiceTest {

    private lateinit var rutingService: RutingService

    @BeforeAll
    fun beforeAll() {
        val rutingGrunnlagIngenInvolvert = RutingGrunnlag(
            søker = false,
            pleietrengende = false,
            annenPart = false
        )

        val lopendeSak = LopendeSakDto(
            søker = "1234", null, null, LocalDate.now(), FagsakYtelseType.UKJENT
        )

        rutingService = RutingService(
            k9SakService = mockk<K9SakService>().also {
                coEvery { it.inngårIUnntaksliste(aktørIder = setOf("888888")) }.returns(true)
                coEvery { it.harLopendeSakSomInvolvererEnAv(lopendeSak.copy(søker = "12345678")) }.returns(
                    rutingGrunnlagIngenInvolvert.copy(søker = true)
                )

                coEvery { it.inngårIUnntaksliste(any()) }.returns(false)
                coEvery { it.harLopendeSakSomInvolvererEnAv(any()) }.returns(rutingGrunnlagIngenInvolvert)
            },
            infotrygdClient = mockk<InfotrygdClient>().also {
                coEvery { it.harLøpendeSakSomInvolvererEnAv(any(), søker = "123456", any(), any(), any()) }.returns(
                    rutingGrunnlagIngenInvolvert.copy(søker = true)
                )
                coEvery { it.harLøpendeSakSomInvolvererEnAv(any(), any(), any(), any(), any()) }.returns(
                    rutingGrunnlagIngenInvolvert
                )
            },
            overstyrTilK9SakJournalpostIds = setOf("573776850".somJournalpostId())
        )
    }

    @Test
    fun `Journalpost som rutes til K9Sak`() {
        val result = runBlocking {
            rutingService.destinasjon(
                søker = "12345678", // http://www.fnrinfo.no/Verktoy/FinnLovlige_Tilfeldig.aspx
                pleietrengende = "05032435485",
                journalpostIds = setOf("123456789"),
                annenPart = null,
                fagsakYtelseType = no.nav.k9punsj.felles.FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE,
                aktørIder = emptySet(),
                fraOgMed = LocalDate.now()
            )
        }

        assertEquals(Destinasjon.K9Sak, result)
    }

    @Test
    fun `Journalpost som rutes til Infotrygd`() {
        val result = runBlocking {
            rutingService.destinasjon(
                søker = "123456", // http://www.fnrinfo.no/Verktoy/FinnLovlige_Tilfeldig.aspx
                pleietrengende = "05032435485",
                journalpostIds = setOf("123456789"),
                annenPart = null,
                fagsakYtelseType = no.nav.k9punsj.felles.FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE,
                aktørIder = emptySet(),
                fraOgMed = LocalDate.now()
            )
        }

        assertEquals(Destinasjon.K9Sak, result)
    }

    @Test
    fun `Journalpost i unntaksliste rutes til K9Sak`() {

        val result = runBlocking {
            rutingService.destinasjon(
                søker = "123456", // http://www.fnrinfo.no/Verktoy/FinnLovlige_Tilfeldig.aspx
                pleietrengende = "05032435485",
                journalpostIds = setOf("123456789"),
                annenPart = null,
                fagsakYtelseType = no.nav.k9punsj.felles.FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE,
                aktørIder = setOf("888888"),
                fraOgMed = LocalDate.now()
            )
        }

        assertEquals(Destinasjon.K9Sak, result)
    }
}
