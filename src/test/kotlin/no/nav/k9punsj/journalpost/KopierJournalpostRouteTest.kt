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
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.OMSORGSPENGER
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.OMSORGSPENGER_UTBETALING
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.PLEIEPENGER_SYKT_BARN
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.UKJENT
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
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDate
import java.time.LocalDateTime

internal class KopierJournalpostRouteTest : AbstractContainerBaseTest() {

    @MockitoBean
    private lateinit var safGateway: SafGateway

    @MockitoBean
    private lateinit var dokarkivGateway: DokarkivGateway

    @MockitoBean
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

    @ParameterizedTest
    @EnumSource(PunsjFagsakYtelseType::class)
    fun `Forventer ingen valideringfeil`(ytelseType: PunsjFagsakYtelseType) {
        val søkerAktørId = "27519339353"

        when (ytelseType) {
            PLEIEPENGER_SYKT_BARN, PLEIEPENGER_LIVETS_SLUTTFASE, OMSORGSPENGER_MIDLERTIDIG_ALENE, OMSORGSPENGER_KRONISK_SYKT_BARN, OMSORGSPENGER_ALENE_OMSORGEN -> {
                val barnEllerAnnenPart = "05032435485"
                KopierJournalpostDto(
                    til = søkerAktørId,
                    barn = barnEllerAnnenPart,
                    annenPart = null,
                    behandlingsÅr = null,
                    ytelse = null
                )
                KopierJournalpostDto(
                    til = søkerAktørId,
                    barn = barnEllerAnnenPart,
                    annenPart = null,
                    behandlingsÅr = null,
                    ytelse = null
                )
                KopierJournalpostDto(
                    til = søkerAktørId,
                    barn = null,
                    annenPart = barnEllerAnnenPart,
                    behandlingsÅr = null,
                    ytelse = null
                )
            }

            else -> {}
        }
        when (ytelseType) {
            OMSORGSPENGER, OMSORGSPENGER_UTBETALING -> {
                val behandlingsÅr = 2024
                KopierJournalpostDto(
                    til = søkerAktørId,
                    barn = null,
                    annenPart = null,
                    behandlingsÅr = behandlingsÅr,
                    ytelse = ytelseType
                )
            }

            else -> {}
        }
    }

    fun `Forventer valideringfeil ved kopiering`() {
        val søkerAktørId = "27519339353"

        // Må sette minst barn eller annenPart uten ytelse satt
        assertThrows<JournalpostkopieringService.KanIkkeKopieresErrorResponse> {
            KopierJournalpostDto(
                til = søkerAktørId,
                barn = null,
                annenPart = null,
                behandlingsÅr = null,
                ytelse = null
            )
        }.also {
            assertThat(it.body.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
            assertThat(it.body.detail).isEqualTo("Må sette minst barn eller annenPart")
        }

        // Må sette minst barn eller annenPart med ytelse satt
        assertThrows<JournalpostkopieringService.KanIkkeKopieresErrorResponse> {
            KopierJournalpostDto(
                til = søkerAktørId,
                barn = null,
                annenPart = null,
                behandlingsÅr = null,
                ytelse = PLEIEPENGER_SYKT_BARN
            )
        }.also {
            assertThat(it.body.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
            assertThat(it.body.detail).isEqualTo("Må sette minst barn eller annenPart")
        }

        // Må sette behandlingsÅr med ytelse satt
        assertThrows<JournalpostkopieringService.KanIkkeKopieresErrorResponse> {
            KopierJournalpostDto(
                til = søkerAktørId,
                barn = null,
                annenPart = null,
                behandlingsÅr = null,
                ytelse = OMSORGSPENGER_UTBETALING
            )
        }.also {
            assertThat(it.body.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
            assertThat(it.body.detail).isEqualTo("Må sette behandlingsÅr")
        }
    }

    @Test
    fun `Mapper kopierjournalpostinfo med barn og sender inn`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val saksnummer = "ABC123"
        val søkerAktørId = "27519339353"
        val barn = "05032435485"

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
            ytelse = "PSB"
        )
        hendelseMottaker.prosesser(melding)

        val journalpost = journalpostRepository.hent(journalpostId)

        val kopierJournalpostDto = KopierJournalpostDto(
            til = journalpost.aktørId.toString(),
            barn = barn,
            annenPart = null,
            behandlingsÅr = null,
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
            .json(
                """
                {
                  "nyJournalpostId":"$nyJournalpostId",
                  "saksnummer":"$saksnummer",
                  "til":"$søkerAktørId",
                  "pleietrengende":"$barn",
                  "annenPart":null,
                  "ytelse":"PSB"
                }""".trimIndent()
            )

        val journalpostKopi = journalpostRepository.hentHvis(nyJournalpostId)
        Assertions.assertNotNull(journalpostKopi)
        assertThat(journalpostKopi!!.type).isNotNull().isEqualTo(K9FordelType.KOPI.kode)
    }

    @Test
    fun `Støtter å overstyre ytelse på journalpost ved kopiering`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val saksnummer = "ABC123"
        val søkerAktørId = "27519339353"
        val barn = "05032435485"

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
            ytelse = UKJENT.kode
        )
        hendelseMottaker.prosesser(melding)

        val journalpost = journalpostRepository.hent(journalpostId)

        val kopierJournalpostDto = KopierJournalpostDto(
            til = journalpost.aktørId.toString(),
            barn = barn,
            annenPart = null,
            behandlingsÅr = null,
            ytelse = PLEIEPENGER_SYKT_BARN
        )

        webTestClient
            .post()
            .uri { it.path("/api/journalpost/kopier/$journalpostId").build() }
            .body(BodyInserters.fromValue(kopierJournalpostDto))
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .json(
                """
                {
                  "nyJournalpostId":"$nyJournalpostId",
                  "saksnummer":"$saksnummer",
                  "til":"$søkerAktørId",
                  "pleietrengende":"$barn",
                  "annenPart":null,
                  "ytelse":"PSB"
                }""".trimIndent()
            )

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
            .header("X-Nav-NorskIdent", journalpost.aktørId!!)
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
            .header("X-Nav-NorskIdent", journalpost.aktørId!!)
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
