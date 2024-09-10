package no.nav.k9punsj.opplaeringspenger

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.sak.kontrakt.opplæringspenger.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjonDto
import no.nav.k9.sak.typer.Periode
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.CALL_ID_KEY
import no.nav.k9punsj.felles.RestKallException
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Urls.hentInstitusjonerUrl
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.net.URI
import java.time.LocalDate
import java.util.*

internal class HentInstitusjonerTest : AbstractContainerBaseTest() {

    @MockkBean
    private lateinit var k9SakService: K9SakService

    private val hentInstitusjonerPunsjUrl = "/api/opplaeringspenger-soknad/institusjoner"
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @BeforeEach
    internal fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }


    @Test
    fun `Forventer at GodkjentOpplæringsinstitusjonDto har riktige typer`(): Unit = runBlocking {
        val correlationId = UUID.randomUUID().toString()
        val uuid = UUID.randomUUID()
        val institusjonNavn = "Sykehus Test"
        val perioder = listOf(Periode(LocalDate.now().minusMonths(12), LocalDate.now()))
        val forventetJson = """
            [
                {
                    "uuid": "$uuid",
                    "navn": "$institusjonNavn",
                    "perioder": [
                        {
                            "fom": "${perioder.first().fom}",
                            "tom": "${perioder.first().tom}"
                        }
                    ]
                }
            ], 
            """.trimIndent()

        val godkjentOpplæringsinstitusjonDto = GodkjentOpplæringsinstitusjonDto(uuid, institusjonNavn, perioder)

        coEvery { k9SakService.hentInstitusjoner() } returns listOf(godkjentOpplæringsinstitusjonDto)

        webTestClient.get()
            .uri { it.path(hentInstitusjonerPunsjUrl).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header(CALL_ID_KEY, correlationId)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .json(forventetJson, true)
    }


    @Test
    fun `Forventer at 500 feil hvis vi får ugyldig response`(): Unit = runBlocking {
        val correlationId = UUID.randomUUID().toString()

        val throwable = Throwable("Feilet ved deserialisering av respons ved henting av institusjoner")
        coEvery { k9SakService.hentInstitusjoner() } throws RestKallException(
            titel = "Feil ved henting av institusjoner",
            message = "Feilet ved deserialisering av respons ved henting av institusjoner: ${throwable.message}",
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            uri = URI.create(hentInstitusjonerUrl)
        )

        val forventetJson = """
            {
                "type": "/problem-details/restkall-feil",
                "title":"Feil ved henting av institusjoner",
                "status":500,
                "detail":"Feilet ved deserialisering av respons ved henting av institusjoner: Feilet ved deserialisering av respons ved henting av institusjoner",
                "instance":"${hentInstitusjonerPunsjUrl}",
                "endepunkt":"${hentInstitusjonerUrl}",
                "correlationId":"${correlationId}"
            }
            """.trimIndent()

        webTestClient.get()
            .uri { it.path(hentInstitusjonerPunsjUrl).build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header(CALL_ID_KEY, correlationId)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectBody()
            .json(forventetJson, true)
    }
}
