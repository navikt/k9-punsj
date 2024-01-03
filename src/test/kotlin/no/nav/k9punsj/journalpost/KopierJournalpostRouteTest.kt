package no.nav.k9punsj.journalpost

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.fordel.FordelPunsjEventDto
import no.nav.k9punsj.fordel.HendelseMottaker
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.journalpost.dto.BehandlingsAarDto
import no.nav.k9punsj.journalpost.dto.KopierJournalpostDto
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDate

internal class KopierJournalpostRouteTest : AbstractContainerBaseTest() {

    @MockBean
    private lateinit var safGateway: SafGateway

    @MockBean
    private lateinit var dokarkivGateway: DokarkivGateway

    @MockBean
    private lateinit var innsendingClient: InnsendingClient

    @MockBean
    private lateinit var soknadService: SoknadService

    @Autowired
    private lateinit var journalpostService: JournalpostService

    @Autowired
    private lateinit var aksjonspunktService: AksjonspunktService

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    private lateinit var hendelseMottaker: HendelseMottaker

    private lateinit var metricsEndpoint: MetricsEndpoint

    private val api = "api"
    private val journalpostUri = "journalpost"
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @BeforeEach
    internal fun setUp() {
        val simpleMeterRegistry = SimpleMeterRegistry()
        hendelseMottaker = HendelseMottaker(
            journalpostService = journalpostService,
            aksjonspunktService = aksjonspunktService,
            meterRegistry = simpleMeterRegistry
        )
        metricsEndpoint = MetricsEndpoint(simpleMeterRegistry)
    }

    @AfterEach
    fun tearDown() {
        cleanUpDB()
    }

    @Test
    fun `Mapper kopierjournalpostinfo med barn og sender inn`(): Unit = runBlocking {

        val journalpostId = IdGenerator.nesteId()
        val melding = FordelPunsjEventDto(
            aktørId = "1234567890",
            journalpostId = journalpostId,
            type = PunsjInnsendingType.PAPIRSØKNAD.kode,
            ytelse = "PSB"
        )
        hendelseMottaker.prosesser(melding)

        val journalpost = journalpostRepository.hent(journalpostId)

        val kopierJournalpostDto = KopierJournalpostDto(
            fra = journalpost.aktørId.toString(),
            til = journalpost.aktørId.toString(),
            barn = "05032435485",
            annenPart = null
        )

        webTestClient
            .post()
            .uri { it.path("/api/journalpost/kopier/$journalpostId").build() }
            .body(BodyInserters.fromValue(kopierJournalpostDto))
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isAccepted
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{}""",
            """{"behandlingsAar": null}""",
            """{"behandlingsAar": 0}""",
            """{"behandlingsAar": -1}""",
        ]
    )
    fun `Sett behandlingår med ugyldige verdier defaulter til nåværende år`(payload: String): Unit = runBlocking {

        val journalpostId = IdGenerator.nesteId()
        val melding = FordelPunsjEventDto(
            aktørId = "1234567890",
            journalpostId = journalpostId,
            type = PunsjInnsendingType.PAPIRSØKNAD.kode,
            ytelse = "PSB"
        )
        hendelseMottaker.prosesser(melding)

        val journalpost = journalpostRepository.hent(journalpostId)
        Assertions.assertNotNull(journalpost)

        webTestClient
            .post()
            .uri { it.path("/api/journalpost/settBehandlingsAar/${journalpostId}").build() }
            .body(BodyInserters.fromValue(payload))
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", journalpost.aktørId)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
                """
                {
                  "behandlingsAar": ${LocalDate.now().year}
                }
                """.trimIndent()
            )
    }

    @Test
    fun `Sett behandlingår med gyldig verdi`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val melding = FordelPunsjEventDto(
            aktørId = "1234567890",
            journalpostId = journalpostId,
            type = PunsjInnsendingType.PAPIRSØKNAD.kode,
            ytelse = "PSB"
        )
        hendelseMottaker.prosesser(melding)

        val journalpost = journalpostRepository.hent(journalpostId)
        Assertions.assertNotNull(journalpost)

        val gyldigÅr = LocalDate.now().year + 1
        val payload = BehandlingsAarDto(behandlingsAar = gyldigÅr)

        webTestClient
            .post()
            .uri { it.path("/api/journalpost/settBehandlingsAar/$journalpostId").build() }
            .body(BodyInserters.fromValue(payload))
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .header("X-Nav-NorskIdent", journalpost.aktørId)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
                """
                {
                  "behandlingsAar": $gyldigÅr
                }
                """.trimIndent()
            )
    }
}
