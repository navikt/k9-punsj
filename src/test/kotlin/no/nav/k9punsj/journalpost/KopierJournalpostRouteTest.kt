package no.nav.k9punsj.journalpost

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.fordel.FordelPunsjEventDto
import no.nav.k9punsj.fordel.HendelseMottaker
import no.nav.k9punsj.fordel.K9FordelType
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.journalpost.dto.BehandlingsAarDto
import no.nav.k9punsj.journalpost.dto.KopierJournalpostDto
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDate
import java.time.LocalDateTime

internal class KopierJournalpostRouteTest : AbstractContainerBaseTest() {

    @MockBean
    private lateinit var safGateway: SafGateway

    @MockBean
    private lateinit var dokarkivGateway: DokarkivGateway

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
        val saksnummer = "ABC123"

        Mockito.`when`(safGateway.hentJournalpostInfo(journalpostId)).thenReturn(
            SafDtos.Journalpost(
                journalpostId = journalpostId,
                tema = null,
                journalstatus = SafDtos.Journalstatus.JOURNALFOERT.name,
                journalposttype = SafDtos.JournalpostType.NOTAT.kode,
                dokumenter = emptyList(),
                relevanteDatoer = emptyList(),
                avsender = null,
                avsenderMottaker = null,
                bruker = SafDtos.Bruker(
                    id = "27519339353",
                    type = "AKTOER_ID"
                ),
                sak = SafDtos.Sak(
                    sakstype = SafDtos.Sakstype.FAGSAK,
                    fagsakId = saksnummer,
                    fagsaksystem = "K9",
                    tema = SafDtos.Tema.OMS.name
                ),
                datoOpprettet = LocalDateTime.now(),
                tittel = null
            )
        )

        val nyJournalpostId = IdGenerator.nesteId()
        Mockito.`when`(
            dokarkivGateway.knyttTilAnnenSak(
                journalpostId = journalpostId.somJournalpostId(),
                identitetsnummer = "27519339353".somIdentitetsnummer(),
                saksnummer = saksnummer
            )
        ).thenReturn(nyJournalpostId.somJournalpostId())

        val melding = FordelPunsjEventDto(
            aktørId = "27519339353",
            journalpostId = journalpostId,
            type = K9FordelType.PAPIRSØKNAD.kode,
            ytelse = "PSB"
        )
        hendelseMottaker.prosesser(melding)

        val journalpost = journalpostRepository.hent(journalpostId)

        val kopierJournalpostDto = KopierJournalpostDto(
            fra = journalpost.aktørId.toString(),
            til = journalpost.aktørId.toString(),
            barn = "05032435485",
            annenPart = null,
            ytelse = null
        )

        webTestClient
            .post()
            .uri { it.path("/api/journalpost/kopier/$journalpostId").build() }
            .body(BodyInserters.fromValue(kopierJournalpostDto))
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .json("""{"nyJournalPostId":"$nyJournalpostId"}""")

        val journalpostKopi = journalpostRepository.hentHvis(nyJournalpostId)
        Assertions.assertNotNull(journalpostKopi)
        assertThat(journalpostKopi!!.type).isNotNull().isEqualTo(K9FordelType.KOPI.kode)
    }

    @Test
    fun `Støtter å overstyre ytelse på journalpost ved kopiering`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val saksnummer = "ABC123"
        val søkerAktørId = "27519339353"

        Mockito.`when`(safGateway.hentJournalpostInfo(journalpostId)).thenReturn(
            SafDtos.Journalpost(
                journalpostId = journalpostId,
                tema = null,
                journalstatus = SafDtos.Journalstatus.JOURNALFOERT.name,
                journalposttype = SafDtos.JournalpostType.NOTAT.kode,
                dokumenter = emptyList(),
                relevanteDatoer = emptyList(),
                avsender = null,
                avsenderMottaker = null,
                bruker = SafDtos.Bruker(
                    id = søkerAktørId,
                    type = "AKTOER_ID"
                ),
                sak = SafDtos.Sak(
                    sakstype = SafDtos.Sakstype.FAGSAK,
                    fagsakId = saksnummer,
                    fagsaksystem = "K9",
                    tema = SafDtos.Tema.OMS.name
                ),
                datoOpprettet = LocalDateTime.now(),
                tittel = null
            )
        )

        val nyJournalpostId = IdGenerator.nesteId()
        Mockito.`when`(
            dokarkivGateway.knyttTilAnnenSak(
                journalpostId = journalpostId.somJournalpostId(),
                identitetsnummer = søkerAktørId.somIdentitetsnummer(),
                saksnummer = saksnummer
            )
        ).thenReturn(nyJournalpostId.somJournalpostId())

        val melding = FordelPunsjEventDto(
            aktørId = søkerAktørId,
            journalpostId = journalpostId,
            type = K9FordelType.PAPIRSØKNAD.kode,
            ytelse = PunsjFagsakYtelseType.UKJENT.kode
        )
        hendelseMottaker.prosesser(melding)

        val journalpost = journalpostRepository.hent(journalpostId)

        val kopierJournalpostDto = KopierJournalpostDto(
            fra = journalpost.aktørId.toString(),
            til = journalpost.aktørId.toString(),
            barn = "05032435485",
            annenPart = null,
            ytelse = PunsjFagsakYtelseType.PLEIEPENGER_SYKT_BARN
        )

        webTestClient
            .post()
            .uri { it.path("/api/journalpost/kopier/$journalpostId").build() }
            .body(BodyInserters.fromValue(kopierJournalpostDto))
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .json("""{"nyJournalPostId":"$nyJournalpostId"}""")

        val journalpostKopi = journalpostRepository.hentHvis(nyJournalpostId)
        Assertions.assertNotNull(journalpostKopi)
        assertThat(journalpostKopi!!.type).isNotNull().isEqualTo(K9FordelType.KOPI.kode)
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
            type = K9FordelType.PAPIRSØKNAD.kode,
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
            type = K9FordelType.PAPIRSØKNAD.kode,
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
