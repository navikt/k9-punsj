package no.nav.k9punsj.fordel

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktServiceImpl
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.rest.eksternt.pdl.TestPdlService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doNothing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        TestBeans::class,
        HendelseMottaker::class,
        AksjonspunktServiceImpl::class,
        JournalpostRepository::class,
        JournalpostService::class,
        SafGateway::class,
        DokarkivGateway::class,
        ObjectMapper::class,
        AksjonspunktRepository::class,
        PersonService::class,
        PersonRepository::class,
        TestPdlService::class,
        SøknadRepository::class,
        AksjonspunktServiceImpl::class,
        SoknadService::class,
        InnsendingClient::class,
        SimpleMeterRegistry::class,
        MockClock::class
    ]
)
internal class FordelKafkaTest {

    @MockBean
    private lateinit var hendelseProducer: HendelseProducer

    @MockBean
    private lateinit var safGateway: SafGateway

    @MockBean
    private lateinit var dokarkivGateway: DokarkivGateway

    @MockBean
    private lateinit var innsendingClient: InnsendingClient

    @MockBean
    private lateinit var soknadService: SoknadService

    @Autowired
    private lateinit var aksjonspunktService: AksjonspunktServiceImpl

    @Autowired
    private lateinit var hendelseMottaker: HendelseMottaker

    @Test
    fun `motta melding om journalføringsoppgave fra fordel`() {
        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666", type = PunsjInnsendingType.PAPIRSØKNAD.kode, ytelse = "PSB")

        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val anyCaptor = ArgumentCaptor.forClass(Any::class.java)

        doNothing().`when`(hendelseProducer).sendMedOnSuccess(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor), onSuccess = captureFun(anyCaptor))

        runBlocking {
            hendelseMottaker.prosesser(melding)
            val punsjJournalpost =
                PunsjJournalpost(UUID.randomUUID(), journalpostId = melding.journalpostId, aktørId = melding.aktørId)
            aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
                punsjJournalpost,
                Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET),
                melding.type,
                melding.ytelse
            )
        }
        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<PunsjEventDto>(value)
        Assertions.assertThat(verdiFraKafka.aktørId).isEqualTo(melding.aktørId)
        Assertions.assertThat(verdiFraKafka.journalpostId).isEqualTo(melding.journalpostId)
        Assertions.assertThat(verdiFraKafka.ytelse).isEqualTo(melding.ytelse)
        Assertions.assertThat(verdiFraKafka.type).isEqualTo(melding.type)
    }

    @Test
    fun `motta melding om journalføringsoppgave fra fordel ukjent aktør`() {
        val melding = FordelPunsjEventDto(journalpostId = "6666", type = "test", ytelse = "test")

        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val anyCaptor = ArgumentCaptor.forClass(Any::class.java)

        doNothing().`when`(hendelseProducer).sendMedOnSuccess(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor), onSuccess = captureFun(anyCaptor))
        runBlocking {
            hendelseMottaker.prosesser(melding)
            val punsjJournalpost =
                PunsjJournalpost(UUID.randomUUID(), journalpostId = melding.journalpostId, aktørId = melding.aktørId)
            aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
                punsjJournalpost,
                Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET),
                melding.type,
                melding.ytelse
            )
        }
        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<PunsjEventDto>(value)
        Assertions.assertThat(verdiFraKafka.aktørId).isNull()
        Assertions.assertThat(verdiFraKafka.journalpostId).isEqualTo(melding.journalpostId)
    }

    private fun captureString(valueCaptor: ArgumentCaptor<String>): String {
        valueCaptor.capture()
        return ""
    }

    private fun captureFun(valueCaptor: ArgumentCaptor<Any>): () -> Unit {
        valueCaptor.capture()
        return {}
    }
}
