package no.nav.k9punsj.akjonspunkter

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.TestContext
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.fordel.FordelPunsjEventDto
import no.nav.k9punsj.fordel.PunsjEventDto
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.objectMapper
import org.assertj.core.api.Assertions.assertThat
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
    AksjonspunktServiceImpl::class,
    TestContext::class,
    JournalpostRepository::class,
    AksjonspunktRepository::class,
    SøknadRepository::class,
    TestBeans::class
])
internal class AksjonspunktServiceImplTest {

    @MockBean
    private lateinit var hendelseProducer: HendelseProducer

    @Autowired
    private lateinit var aksjonspunktService: AksjonspunktServiceImpl

    @Autowired
    private lateinit var aksjonspunktRepository: AksjonspunktRepository

    @Autowired
    private lateinit var journalpostRepository: JournalpostRepository

    @Test
    fun `opprett aksjonspunkt og deretter sett på vent`(): Unit = runBlocking {
        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666")

        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val anyCaptor = ArgumentCaptor.forClass(Any::class.java)

        journalpostRepository.opprettJournalpost(Journalpost(UUID.randomUUID(), journalpostId = melding.journalpostId, aktørId = melding.aktørId))

        aksjonspunktRepository.opprettAksjonspunkt(AksjonspunktEntitet(
                aksjonspunktId = UUID.randomUUID().toString(),
        aksjonspunktKode = AksjonspunktKode.PUNSJ,
        journalpostId = melding.journalpostId,
        aksjonspunktStatus = AksjonspunktStatus.OPPRETTET))

        val hentAlleAksjonspunkter = aksjonspunktRepository.hentAlleAksjonspunkter(melding.journalpostId)
        assertThat(hentAlleAksjonspunkter).hasSize(1)

        Mockito.doNothing().`when`(hendelseProducer).sendMedOnSuccess(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor), onSuccess = captureFun(anyCaptor))
        aksjonspunktService.settPåVentOgSendTilLos(melding.journalpostId, "21707da8-a13b-4927-8776-c53399727b29")

        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<PunsjEventDto>(value)
        assertThat(verdiFraKafka.aksjonspunktKoderMedStatusListe).isEqualTo(mutableMapOf(Pair("PUNSJ", "UTFO"), Pair("MER_INFORMASJON", "OPPR")))
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
