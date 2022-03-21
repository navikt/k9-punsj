package no.nav.k9punsj.rest.eksternt.punsjbollen

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenResponse
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.db.datamodell.Person
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.integrasjoner.punsjbollen.PunsjbolleRuting
import no.nav.k9punsj.integrasjoner.punsjbollen.RestPunsjbolleService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RestPunsjbolleServiceTest {

    private lateinit var wiremockServer: WireMockServer
    private lateinit var punsjbolleService: RestPunsjbolleService

    @Test
    fun `Journalpost som rutes til K9Sak`() {
        assertEquals(ruting(TilK9Sak), PunsjbolleRuting.K9Sak)
    }

    @Test
    fun `Journalpost som rutes til Infotrygd`() {
        assertEquals(ruting(TilInfotrygd), PunsjbolleRuting.Infotrygd)
    }

    @Test
    fun `Journalpost som ikke støttes`() {
        assertEquals(ruting(IkkeStøttet), PunsjbolleRuting.IkkeStøttet)
    }

    @Test
    fun `Uventet response`() {
        assertThrows<IllegalStateException> { ruting(UventetResponse) }
    }

    private fun ruting(correlationId: CorrelationId) = runBlocking {
        punsjbolleService.ruting(
            søker = "123",
            pleietrengende = "456",
            journalpostId = "789",
            annenPart = null,
            periode = null,
            correlationId = correlationId,
            fagsakYtelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN
        )
    }


    @BeforeAll
    fun beforeAll() {
        wiremockServer = WireMockBuilder()
            .build()
            .stubTilK9Sak()
            .stubTilInfotrygd()
            .stubIkkeStøttet()
            .stubUventetResponse()

        punsjbolleService = RestPunsjbolleService(
            baseUrl = URI(wiremockServer.baseUrl()),
            scope = "k9-punsjbolle",
            accessTokenClient = mockk<AccessTokenClient>().also {
                coEvery { it.getAccessToken(any()) }.returns(AccessTokenResponse(
                    accessToken = "foo",
                    expiresIn = 1000L,
                    tokenType = "Bearer"
                ))
            },
            personService = mockk<PersonService>().also {
                coEvery { it.finnEllerOpprettPersonVedNorskIdent(any()) }.returns(Person(
                    personId = "1234",
                    aktørId = "5678",
                    norskIdent = "9101112"
                ))
            }
        )

    }

    @AfterAll
    fun afterAll() {
        wiremockServer.stop()
    }

    private companion object {
        private const val TilK9Sak = "1"
        private const val TilInfotrygd = "2"
        private const val IkkeStøttet = "3"
        private const val UventetResponse = "4"


        private fun WireMockServer.stubTilK9Sak() = stubPunsjbolleRuting(
            correlationId = TilK9Sak,
            httpStatus = 200,
            responseBody = """{"destinasjon":"K9Sak"}"""
        )

        private fun WireMockServer.stubTilInfotrygd() = stubPunsjbolleRuting(
            correlationId = TilInfotrygd,
            httpStatus = 200,
            responseBody = """{"destinasjon":"Infotrygd"}"""
        )

        private fun WireMockServer.stubIkkeStøttet() = stubPunsjbolleRuting(
            correlationId = IkkeStøttet,
            httpStatus = 409,
            responseBody = """{"type":"punsjbolle://ikke-støttet-journalpost"}"""
        )

        private fun WireMockServer.stubUventetResponse() = stubPunsjbolleRuting(
            correlationId = UventetResponse,
            httpStatus = 500,
            responseBody = "Noe gikk gæli",
            contentType = "text/plain"
        )

        private fun WireMockServer.stubPunsjbolleRuting(
            correlationId: CorrelationId,
            httpStatus: Int,
            contentType: String = "application/json",
            responseBody: String,
        ): WireMockServer {
            WireMock.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/ruting"))
                    .withHeader("Content-Type", WireMock.equalTo("application/json"))
                    .withHeader("Accept", WireMock.equalTo("application/json"))
                    .withHeader("Authorization", WireMock.equalTo("Bearer foo"))
                    .withHeader("X-Correlation-Id", WireMock.equalTo(correlationId))
                    .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", contentType)
                        .withBody(responseBody)
                        .withStatus(httpStatus)
                    )
            )
            return this
        }
    }
}
