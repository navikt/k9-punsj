package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9.formidling.kontrakt.kodeverk.IdType
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.brev.dto.BrevType
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.felles.dto.Person
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.util.IdGenerator
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = ["spring.config.location = classpath:application.yml"])
@ContextConfiguration(classes = [
    JournalpostRepository::class,
    BrevRepository::class,
    BrevServiceImpl::class,
    TestBeans::class
])
internal class BrevServiceImplTest {

    @MockBean
    private lateinit var hendelseProducer: HendelseProducer

    @MockBean
    private lateinit var journalpostService: JournalpostService

    @MockBean
    private lateinit var personService: PersonService


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
        Mockito.doAnswer { true }.`when`(journalpostService).kanSendeInn(Mockito.anyList())
        Mockito.doAnswer { Person("123", "1234", "1000000000000") }.`when`(personService).finnPersonVedNorskIdentFørstDbSåPdl("1234")

        val forJournalpostId = IdGenerator.nesteId()
        val saksnummer = "123"
        val dokumentbestilling = lagDokumentbestillingPåJournalpost(forJournalpostId)

        // act
        brev.bestillBrev(forJournalpostId, dokumentbestilling, BrevType.FRITEKSTBREV, "saksbehandler")

        // assert
        Assertions.assertThat(topicCaptor.value).isEqualTo("privat-k9-dokumenthendelse")
        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<Dokumentbestilling>(value)

        Assertions.assertThat(verdiFraKafka.aktørId).isEqualTo("1000000000000")
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

    private fun lagDokumentbestillingPåJournalpost(forJournalpostId: String): DokumentbestillingDto {
        return DokumentbestillingDto(
            forJournalpostId,
            "2",
            "123",
            "1234",
            DokumentbestillingDto.Mottaker(IdType.ORGNR.name, "Statnett"),
            FagsakYtelseType.OMSORGSPENGER,
            "INNTID"
        )
    }
}
