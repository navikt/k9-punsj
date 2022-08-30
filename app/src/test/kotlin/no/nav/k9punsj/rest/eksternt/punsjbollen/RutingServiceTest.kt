package no.nav.k9punsj.rest.eksternt.punsjbollen

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.integrasjoner.infotrygd.InfotrygdClient
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.ruting.RutingService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RutingServiceTest {

    private lateinit var wiremockServer: WireMockServer
    private lateinit var rutingService: RutingService

    @Test
    fun `Journalpost som rutes til K9Sak`() {
        assertEquals(ruting(TilK9Sak), RutingService.Destinasjon.K9Sak)
    }

    @Test
    fun `Journalpost som rutes til Infotrygd`() {
        assertEquals(ruting(TilInfotrygd), RutingService.Destinasjon.Infotrygd)
    }

    private fun ruting(journalpostId: String) = runBlocking {
        rutingService.destinasjon(
            søker = "05032435485".somIdentitetsnummer(), // http://www.fnrinfo.no/Verktoy/FinnLovlige_Tilfeldig.aspx
            pleietrengende = "05032435485".somIdentitetsnummer(),
            journalpostIds = setOf(journalpostId.somJournalpostId()),
            annenPart = null,
            fagsakYtelseType = no.nav.k9punsj.felles.FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            aktørIder = emptySet(),
            fraOgMed = LocalDate.now()
        )
    }

    @BeforeAll
    fun beforeAll() {
        wiremockServer = WireMockBuilder()
            .build()
            .stubTilK9Sak()
            .stubTilInfotrygd()

        rutingService = RutingService(
            k9SakService = mockk<K9SakService>(),
            infotrygdClient = mockk<InfotrygdClient>(),
            overstyrTilK9SakJournalpostIds = setOf("573776850".somJournalpostId())
        )
    }

    @AfterAll
    fun afterAll() {
        wiremockServer.stop()
    }

    private companion object {
        private const val TilK9Sak = "1234567890"
        private const val TilInfotrygd = "0987654321"

        private fun WireMockServer.stubTilK9Sak() = stubRutingService(
            httpStatus = 200,
            responseBody = """{"destinasjon":"K9Sak"}""",
            forventetRuting = TilK9Sak
        )

        private fun WireMockServer.stubTilInfotrygd() = stubRutingService(
            httpStatus = 200,
            responseBody = """{"destinasjon":"Infotrygd"}""",
            forventetRuting = TilInfotrygd
        )

        private fun WireMockServer.stubRutingService(
            httpStatus: Int,
            contentType: String = "application/json",
            responseBody: String,
            forventetRuting: String
        ): WireMockServer {
            WireMock.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/ruting"))
                    .withHeader("Content-Type", WireMock.equalTo("application/json"))
                    .withHeader("Accept", WireMock.equalTo("application/json"))
                    .withHeader("Authorization", WireMock.equalTo("Bearer foo"))
                    .withRequestBody(WireMock.containing(forventetRuting))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", contentType)
                            .withBody(responseBody)
                            .withStatus(httpStatus)
                    )
            )
            return this
        }
    }
}
