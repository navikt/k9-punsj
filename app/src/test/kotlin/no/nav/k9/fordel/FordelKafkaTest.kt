package no.nav.k9.fordel

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.k9.TestContext
import no.nav.k9.journalpost.JournalpostRepository
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.objectMapper
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
@ContextConfiguration(classes = [HendelseMottaker::class, TestContext::class, JournalpostRepository::class])
internal class FordelKafkaTest {
    @MockBean
    private lateinit var hendelseProducer: HendelseProducer

    @Autowired
    private lateinit var hendelseMottaker: HendelseMottaker

    @Test
    fun `motta melding om journalføringsoppgave fra fordel`() {
        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "200")

        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)

        
        doNothing().`when`(hendelseProducer).send(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor))
        runBlocking {
            hendelseMottaker.prosesser(melding.journalpostId, melding.aktørId)
        }
        Assertions.assertThat(topicCaptor.value).isEqualTo("privat-k9punsj-aksjonspunkthendelse-v1")
        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<PunsjEventDto>(value)
        Assertions.assertThat(verdiFraKafka.aktørId).isEqualTo(melding.aktørId)
        Assertions.assertThat(verdiFraKafka.journalpostId).isEqualTo(melding.journalpostId)
    }

    @Test
    fun `motta melding om journalføringsoppgave fra fordel ukjent aktør`() {
        val melding = FordelPunsjEventDto(journalpostId = "200")

        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)

        
        doNothing().`when`(hendelseProducer).send(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor))
        runBlocking {
            hendelseMottaker.prosesser(melding.journalpostId, melding.aktørId)
        }
        Assertions.assertThat(topicCaptor.value).isEqualTo("privat-k9punsj-aksjonspunkthendelse-v1")
        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<PunsjEventDto>(value)
        Assertions.assertThat(verdiFraKafka.aktørId).isNull()
        Assertions.assertThat(verdiFraKafka.journalpostId).isEqualTo(melding.journalpostId)
    }

    private fun captureString(valueCaptor: ArgumentCaptor<String>): String {
        valueCaptor.capture()
        return ""
    }
}