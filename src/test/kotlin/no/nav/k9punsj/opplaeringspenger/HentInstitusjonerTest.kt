package no.nav.k9punsj.opplaeringspenger

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.sak.kontrakt.opplæringspenger.godkjentopplaeringsinstitusjon.GodkjentOpplæringsinstitusjonDto
import no.nav.k9.sak.typer.Periode
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.CALL_ID_KEY
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.*

internal class HentInstitusjonerTest : AbstractContainerBaseTest() {

    @MockkBean
    private lateinit var k9SakService: K9SakService

    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @BeforeEach
    internal fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `Forventer at GodkjentOpplæringsinstitusjonDto har riktige typer`(): Unit = runBlocking {
        val uuid = UUID.randomUUID()
        val institusjonNavn = "Sykehus Test"
        val perioder = listOf(Periode(LocalDate.now().minusMonths(12), LocalDate.now()))
        val gyldigJson = """
                {
                    "first": 
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
                    "second": null
                }
                """.trimIndent()
        val correlationId = UUID.randomUUID().toString()

        val godkjentOpplæringsinstitusjonDto = GodkjentOpplæringsinstitusjonDto(uuid, institusjonNavn, perioder)

        coEvery { k9SakService.hentInstitusjoner() } returns Pair(listOf(godkjentOpplæringsinstitusjonDto), null)

        webTestClient.get()
            .uri { it.path("/api/opplaeringspenger-soknad/institusjoner").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header(CALL_ID_KEY, correlationId)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody()
            .json(gyldigJson, true)
    }

    @Test
    fun `Forventer at 500 feil hvis vi får ugyldig response`(): Unit = runBlocking {
        val correlationId = UUID.randomUUID().toString()

        val forventetJson = """
                {
                    "feil": "Feilet deserialisering"
                }
                """.trimIndent()

        coEvery { k9SakService.hentInstitusjoner() } returns Pair(null, "Feilet deserialisering")

        webTestClient.get()
            .uri { it.path("/api/opplaeringspenger-soknad/institusjoner").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header(CALL_ID_KEY, correlationId)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectBody()
            .json(forventetJson, true)
    }
}
