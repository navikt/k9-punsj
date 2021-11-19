package no.nav.k9punsj.brev

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@ContextConfiguration(classes = [
    JournalpostRepository::class,
    BrevRepository::class,
    BrevServiceImpl::class,
    TestBeans::class
])
internal class BrevServiceImplTest {

    @MockBean
    private lateinit var hendelseProducer: HendelseProducer

    @Autowired
    private lateinit var brev: BrevServiceImpl

    @Test
    fun `opprett brev og send brevbestilling på kafka`(): Unit = runBlocking {
        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val anyCaptor = ArgumentCaptor.forClass(Any::class.java)

        Mockito.doNothing().`when`(hendelseProducer).sendMedOnSuccess(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor), onSuccess = captureFun(anyCaptor))

        brev.bestillBrev()

        Assertions.assertThat(topicCaptor.value).isEqualTo(Topics.SEND_BREVBESTILLING_TIL_K9_FORMIDLING)
//        val value = valueCaptor.value
//        val verdiFraKafka = objectMapper().readValue<PunsjEventDto>(value)
//        Assertions.assertThat(verdiFraKafka.aktørId).isNull()
//        Assertions.assertThat(verdiFraKafka.journalpostId).isEqualTo(melding.journalpostId)
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
