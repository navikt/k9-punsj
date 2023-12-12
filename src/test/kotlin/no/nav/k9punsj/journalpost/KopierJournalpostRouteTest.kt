package no.nav.k9punsj.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktServiceImpl
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.fordel.FordelPunsjEventDto
import no.nav.k9punsj.fordel.HendelseMottaker
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.journalpost.dto.BehandlingsAarDto
import no.nav.k9punsj.journalpost.dto.KopierJournalpostDto
import no.nav.k9punsj.rest.eksternt.pdl.TestPdlService
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.WebClientUtils.postAndAssert
import no.nav.k9punsj.util.WebClientUtils.postAndAssertAwaitWithStatusAndBody
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        TestBeans::class,
        AksjonspunktServiceImpl::class,
        JournalpostRepository::class,
        JournalpostService::class,
        SafGateway::class,
        DokarkivGateway::class,
        ObjectMapper::class,
        AksjonspunktRepository::class,
        AksjonspunktServiceImpl::class,
        SoknadService::class,
        InnsendingClient::class,
        PersonService::class,
        PersonRepository::class,
        TestPdlService::class,
        SøknadRepository::class
    ]
)
internal class KopierJournalpostRouteTest {

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
    private lateinit var hendelseMottaker: HendelseMottaker

    private lateinit var metricsEndpoint: MetricsEndpoint

    private val client = TestSetup.client
    private val api = "api"
    private val journalpostUri = "journalpost"
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    private val journalpostRepository = DatabaseUtil.getJournalpostRepo()

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
        DatabaseUtil.cleanDB()
    }

    @Test
    fun `Mapper kopierjournalpostinfo med barn og sender inn`() = runBlocking {

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

        val body = client.postAndAssert(
                authorizationHeader = saksbehandlerAuthorizationHeader,
                assertStatus = HttpStatus.ACCEPTED,
                requestBody = BodyInserters.fromValue(kopierJournalpostDto),
                api,
                journalpostUri,
                "kopier",
                journalpostId
            )

        Assertions.assertTrue(body.statusCode().is2xxSuccessful)
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
    fun `Sett behandlingår med ugyldige verdier defaulter til nåværende år`(payload: String) = runBlocking {

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

        val body = client.postAndAssertAwaitWithStatusAndBody<String, String>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            navNorskIdentHeader = journalpost.aktørId,
            assertStatus = HttpStatus.OK,
            requestBody = BodyInserters.fromValue(payload),
            api,
            journalpostUri,
            "settBehandlingsAar",
            journalpostId
        )

        val forventetRespons = """
            {
              "behandlingsAar": ${LocalDate.now().year}
            }""".trimIndent()

        JSONAssert.assertEquals(forventetRespons, body, true)
    }

    @Test
    fun `Sett behandlingår med gyldig verdi`() = runBlocking {
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

        val body = client.postAndAssertAwaitWithStatusAndBody<BehandlingsAarDto, String>(
            authorizationHeader = saksbehandlerAuthorizationHeader,
            navNorskIdentHeader = journalpost.aktørId,
            assertStatus = HttpStatus.OK,
            requestBody = BodyInserters.fromValue(payload),
            api,
            journalpostUri,
            "settBehandlingsAar",
            journalpostId
        )

        val forventetRespons = """
        {
          "behandlingsAar": $gyldigÅr
        }""".trimIndent()

        JSONAssert.assertEquals(forventetRespons, body, true)
    }
}
