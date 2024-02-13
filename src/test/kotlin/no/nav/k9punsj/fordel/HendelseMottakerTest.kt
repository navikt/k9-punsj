package no.nav.k9punsj.fordel

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.metrikker.Metrikk
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.MetricUtils.Companion.assertCounter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

internal class HendelseMottakerTest: AbstractContainerBaseTest() {

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
    lateinit var aksjonspunktRepository: AksjonspunktRepository

    private lateinit var hendelseMottaker: HendelseMottaker

    private lateinit var metricsEndpoint: MetricsEndpoint

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

    @Test
    fun `skal lagre ned informasjon om journalpost`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()

        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = K9FordelType.PAPIRSØKNAD.kode, ytelse = "PSB")
        hendelseMottaker.prosesser(melding)

        val journalpost = journalpostService.hent(journalpostId)
        assertThat(journalpost).isNotNull

        assertCounter(
            metricsEndpoint = metricsEndpoint,
            metric = Metrikk.ANTALL_OPPRETTET_JOURNALPOST_COUNTER,
            forventetVerdi = 1.0
        )
    }

    @Test
    fun `skal ikke lagre ned informasjon om journalpost når det kommer samme uten status`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val meldingSomIkkeSkalBrukes = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = K9FordelType.PAPIRSØKNAD.kode, ytelse = "PSB")

        val punsjJournalpostTilDb = PunsjJournalpost(UUID.randomUUID(), journalpostId = meldingSomIkkeSkalBrukes.journalpostId, aktørId = meldingSomIkkeSkalBrukes.aktørId, type = K9FordelType.DIGITAL_ETTERSENDELSE.kode)
        journalpostService.lagre(punsjJournalpostTilDb)

        hendelseMottaker.prosesser(meldingSomIkkeSkalBrukes)

        val journalpost = journalpostService.hent(journalpostId)
        assertThat(journalpost.type).isEqualTo(K9FordelType.DIGITAL_ETTERSENDELSE.kode)
    }

    @Test
    fun `skal ikke lagre ned informasjon om journalpost når journalposten har ankommet fra før med samme status`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val meldingSomIkkeSkalBrukes = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = K9FordelType.PAPIRSØKNAD.kode, ytelse = "PSB")

        val punsjJournalpostTilDb = PunsjJournalpost(UUID.randomUUID(), journalpostId = meldingSomIkkeSkalBrukes.journalpostId, aktørId = meldingSomIkkeSkalBrukes.aktørId, type = K9FordelType.DIGITAL_ETTERSENDELSE.kode)
        journalpostService.lagre(punsjJournalpostTilDb)

        hendelseMottaker.prosesser(meldingSomIkkeSkalBrukes)

        val journalpost = journalpostService.hent(journalpostId)
        assertThat(journalpost.type).isEqualTo(K9FordelType.DIGITAL_ETTERSENDELSE.kode)
    }

    @Test
    fun `skal fjerne oppgave når det kommer info fra fordel`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val førsteMelding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = K9FordelType.INNTEKTSMELDING_UTGÅTT.kode, ytelse = "PSB")

        hendelseMottaker.prosesser(førsteMelding)

        val meldingSomSkalLukkeOppgave = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = K9FordelType.PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG.kode, ytelse = "PSB")

        hendelseMottaker.prosesser(meldingSomSkalLukkeOppgave)

        val journalpost = journalpostService.hent(meldingSomSkalLukkeOppgave.journalpostId)
        assertThat(journalpost.type).isEqualTo(K9FordelType.PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG.kode)

        val alleAksjonspunkter = aksjonspunktRepository.hentAlleAksjonspunkter(journalpostId = førsteMelding.journalpostId)

        assertThat(alleAksjonspunkter).hasSize(1)
        assertThat(alleAksjonspunkter[0].aksjonspunktStatus).isEqualTo(AksjonspunktStatus.UTFØRT)
    }
}
