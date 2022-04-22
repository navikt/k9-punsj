package no.nav.k9punsj.fordel

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktServiceImpl
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.metrikker.Metrikk
import no.nav.k9punsj.rest.eksternt.pdl.TestPdlService
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.MetricUtils.Companion.assertCounter
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*


@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@ContextConfiguration(classes = [
    TestBeans::class,
    AksjonspunktServiceImpl::class,
    JournalpostRepository::class,
    AksjonspunktRepository::class,
    PersonService::class,
    PersonRepository::class,
    TestPdlService::class,
    SøknadRepository::class
])
internal class HendelseMottakerTest {

    @Autowired
    private lateinit var journalpostRepository: JournalpostRepository

    @Autowired
    private lateinit var aksjonspunktService: AksjonspunktService

    private lateinit var hendelseMottaker: HendelseMottaker

    private lateinit var metricsEndpoint: MetricsEndpoint

    @BeforeEach
    internal fun setUp() {
        val simpleMeterRegistry = SimpleMeterRegistry()
        hendelseMottaker = HendelseMottaker(
            journalpostRepository = journalpostRepository,
            aksjonspunktService = aksjonspunktService,
            meterRegistry = simpleMeterRegistry
        )

        metricsEndpoint = MetricsEndpoint(simpleMeterRegistry)
    }

    @Test
    fun `skal lagre ned informasjon om journalpost`() : Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()

        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = PunsjInnsendingType.PAPIRSØKNAD.kode, ytelse = "PSB")
        hendelseMottaker.prosesser(melding)

        val journalpost = hendelseMottaker.journalpostRepository.hent(journalpostId)
        assertThat(journalpost).isNotNull

        assertCounter(
            metricsEndpoint = metricsEndpoint,
            metric = Metrikk.ANTALL_OPPRETTET_JOURNALPOST_COUNTER,
            forventetVerdi = 1.0
        )
    }

    @Test
    fun `skal ikke lagre ned informasjon om journalpost når det kommer samme uten status`() : Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val journalpostRepository = hendelseMottaker.journalpostRepository
        val meldingSomIkkeSkalBrukes = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = PunsjInnsendingType.PAPIRSØKNAD.kode, ytelse = "PSB")

        val punsjJournalpostTilDb = PunsjJournalpost(UUID.randomUUID(), journalpostId = meldingSomIkkeSkalBrukes.journalpostId, aktørId = meldingSomIkkeSkalBrukes.aktørId, type = PunsjInnsendingType.DIGITAL_ETTERSENDELSE.kode)
        journalpostRepository.lagre(punsjJournalpostTilDb){
            punsjJournalpostTilDb
        }

        hendelseMottaker.prosesser(meldingSomIkkeSkalBrukes)

        val journalpost = journalpostRepository.hent(journalpostId)
        Assertions.assertThat(journalpost.type).isEqualTo(PunsjInnsendingType.DIGITAL_ETTERSENDELSE.kode)
    }

    @Test
    fun `skal ikke lagre ned informasjon om journalpost når journalposten har ankommet fra før med samme status`() : Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val journalpostRepository = hendelseMottaker.journalpostRepository
        val meldingSomIkkeSkalBrukes = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = PunsjInnsendingType.PAPIRSØKNAD.kode, ytelse = "PSB")

        val punsjJournalpostTilDb = PunsjJournalpost(UUID.randomUUID(), journalpostId = meldingSomIkkeSkalBrukes.journalpostId, aktørId = meldingSomIkkeSkalBrukes.aktørId, type = PunsjInnsendingType.DIGITAL_ETTERSENDELSE.kode)
        journalpostRepository.lagre(punsjJournalpostTilDb){
            punsjJournalpostTilDb
        }

        hendelseMottaker.prosesser(meldingSomIkkeSkalBrukes)

        val journalpost = journalpostRepository.hent(journalpostId)
        Assertions.assertThat(journalpost.type).isEqualTo(PunsjInnsendingType.DIGITAL_ETTERSENDELSE.kode)
    }

    @Test
    fun `skal fjerne oppgave når det kommer info fra fordel`(): Unit = runBlocking {
        val journalpostId = IdGenerator.nesteId()
        val journalpostRepository = hendelseMottaker.journalpostRepository
        val førsteMelding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = PunsjInnsendingType.INNTEKTSMELDING_UTGÅTT.kode, ytelse = "PSB")

        hendelseMottaker.prosesser(førsteMelding)

        val meldingSomSkalLukkeOppgave = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = journalpostId, type = PunsjInnsendingType.PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG.kode, ytelse = "PSB")

        hendelseMottaker.prosesser(meldingSomSkalLukkeOppgave)

        val journalpost = journalpostRepository.hent(meldingSomSkalLukkeOppgave.journalpostId)
        Assertions.assertThat(journalpost.type).isEqualTo(PunsjInnsendingType.PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG.kode)

        val alleAksjonspunkter =
            DatabaseUtil.getAksjonspunktRepo().hentAlleAksjonspunkter(journalpostId = førsteMelding.journalpostId)

        Assertions.assertThat(alleAksjonspunkter).hasSize(1)
        Assertions.assertThat(alleAksjonspunkter[0].aksjonspunktStatus).isEqualTo(AksjonspunktStatus.UTFØRT)
    }
}
