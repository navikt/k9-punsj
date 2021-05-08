package no.nav.k9punsj.fordel

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.TestContext
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktServiceImpl
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
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

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@ContextConfiguration(classes = [HendelseMottaker::class, AksjonspunktServiceImpl::class, TestContext::class, JournalpostRepository::class, AksjonspunktRepository::class])
internal class FordelKafkaTest {

    @MockBean
    private lateinit var hendelseProducer: HendelseProducer

    @Autowired
    private lateinit var aksjonspunktService: AksjonspunktServiceImpl

    @Autowired
    private lateinit var hendelseMottaker: HendelseMottaker

    @Test
    fun `motta melding om journalføringsoppgave fra fordel`() {
        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666")

        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val anyCaptor = ArgumentCaptor.forClass(Any::class.java)

        doNothing().`when`(hendelseProducer).sendMedOnSuccess(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor), onSuccess = captureFun(anyCaptor))

        runBlocking {
            hendelseMottaker.prosesser(melding.journalpostId, melding.aktørId)
            aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(melding.journalpostId, Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET))
        }
        Assertions.assertThat(topicCaptor.value).isEqualTo(Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS)
        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<PunsjEventDto>(value)
        Assertions.assertThat(verdiFraKafka.aktørId).isEqualTo(melding.aktørId)
        Assertions.assertThat(verdiFraKafka.journalpostId).isEqualTo(melding.journalpostId)
    }

    @Test
    fun `motta melding om journalføringsoppgave fra fordel ukjent aktør`() {
        val melding = FordelPunsjEventDto(journalpostId = "6666")

        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val anyCaptor = ArgumentCaptor.forClass(Any::class.java)

        doNothing().`when`(hendelseProducer).sendMedOnSuccess(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor), onSuccess = captureFun(anyCaptor))
        runBlocking {
            hendelseMottaker.prosesser(melding.journalpostId, melding.aktørId)
            aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(melding.journalpostId, Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET))
        }
        Assertions.assertThat(topicCaptor.value).isEqualTo(Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS)
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
