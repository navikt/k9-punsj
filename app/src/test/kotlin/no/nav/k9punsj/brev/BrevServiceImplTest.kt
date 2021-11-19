package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9.formidling.kontrakt.kodeverk.IdType
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.JournalpostId
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
import java.util.UUID


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
        // arrange
        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val anyCaptor = ArgumentCaptor.forClass(Any::class.java)

        Mockito.doNothing().`when`(hendelseProducer).sendMedOnSuccess(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor), onSuccess = captureFun(anyCaptor))

        val forJournalpostId = "1234"
        val aktørId = "AktørId"
        val saksnummer = "123"
        val dokumentbestilling = lagDokumentbestillingPåJournalpost(forJournalpostId, saksnummer, aktørId)

        val brevEntitet =
            BrevEntitet(UUID.randomUUID().toString(), forJournalpostId, dokumentbestilling, BrevType.FRITEKSTBREV)

        // act
        brev.bestillBrev(brevEntitet)

        // assert
        Assertions.assertThat(topicCaptor.value).isEqualTo(Topics.SEND_BREVBESTILLING_TIL_K9_FORMIDLING)
        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<Dokumentbestilling>(value)

        Assertions.assertThat(verdiFraKafka.aktørId).isEqualTo(aktørId)
        Assertions.assertThat(verdiFraKafka.saksnummer).isEqualTo(saksnummer)
    }

    private fun captureString(valueCaptor: ArgumentCaptor<String>): String {
        valueCaptor.capture()
        return ""
    }

    private fun captureFun(valueCaptor: ArgumentCaptor<Any>): () -> Unit {
        valueCaptor.capture()
        return {}
    }

    private fun lagDokumentbestillingPåJournalpost(
        journalpostId: JournalpostId,
        saksnummer: String?,
        aktørId: AktørId,
    ): DokumentbestillingDto {
        val brevData = DokumentbestillingDto("1", "2", "123", "1234", DokumentbestillingDto.Mottaker(IdType.ORGNR.name, "Statnett"), FagsakYtelseType.OMSORGSPENGER, "2", "2")
        return brevData
    }
}
