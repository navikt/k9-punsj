package no.nav.k9punsj.akjonspunkter

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.domenetjenester.SoknadService
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.fordel.FordelPunsjEventDto
import no.nav.k9punsj.integrasjoner.k9losapi.PunsjEventDto
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.metrikker.SøknadMetrikkService
import no.nav.k9punsj.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

internal class AksjonspunktServiceImplTest: AbstractContainerBaseTest() {

    @MockBean
    private lateinit var hendelseProducer: HendelseProducer

    @MockBean
    private lateinit var safGateway: SafGateway

    @MockBean
    private lateinit var dokarkivGateway: DokarkivGateway

    @MockBean
    private lateinit var innsendingClient: InnsendingClient

    @MockBean
    private lateinit var søknadMetrikkService: SøknadMetrikkService

    @MockBean
    private lateinit var k9SakService: K9SakService

    @Autowired
    private lateinit var soknadService: SoknadService

    @Autowired
    private lateinit var aksjonspunktService: AksjonspunktServiceImpl

    @Autowired
    private lateinit var aksjonspunktRepository: AksjonspunktRepository

    @Autowired
    private lateinit var mappeRepository: MappeRepository

    @Autowired
    private lateinit var bunkeRepository: BunkeRepository

    @Autowired
    private lateinit var søknadRepository: SøknadRepository

    @Autowired
    private lateinit var personRepository: PersonRepository

    @Autowired
    private lateinit var journalpostService: JournalpostService

    @Test
    fun `opprett aksjonspunkt og deretter sett på vent`(): Unit = runBlocking {
        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666", type = "test", ytelse = "test")

        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val anyCaptor = ArgumentCaptor.forClass(Any::class.java)

        journalpostService.opprettJournalpost(PunsjJournalpost(UUID.randomUUID(), journalpostId = melding.journalpostId, aktørId = melding.aktørId))

        aksjonspunktRepository.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = UUID.randomUUID().toString(),
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        val hentAlleAksjonspunkter = aksjonspunktRepository.hentAlleAksjonspunkter(melding.journalpostId)
        assertThat(hentAlleAksjonspunkter).hasSize(1)

        Mockito.doNothing().`when`(hendelseProducer).sendMedOnSuccess(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor), onSuccess = captureFun(anyCaptor))
        aksjonspunktService.settPåVentOgSendTilLos(melding.journalpostId, "21707da8-a13b-4927-8776-c53399727b29")

        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<PunsjEventDto>(value)
        assertThat(verdiFraKafka.aksjonspunktKoderMedStatusListe).isEqualTo(mutableMapOf(Pair("PUNSJ", "UTFO"), Pair("MER_INFORMASJON", "OPPR")))
    }

    @Test
    fun `skal sende riktig aktørId til los hvis en søknad har blitt flippet fra opprinnelig aktørId til en ny på søknaden`(): Unit = runBlocking {
        val barnetsAktørId = "235612324"
        val morsAktørId = "1253124234"
        val journalpostId = "294523"
        val søknadId = "21707da8-a13b-4927-8776-c53399727b29"

        val melding = FordelPunsjEventDto(aktørId = barnetsAktørId, journalpostId = journalpostId, type = "test", ytelse = "test")

        val topicCaptor = ArgumentCaptor.forClass(String::class.java)
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val anyCaptor = ArgumentCaptor.forClass(Any::class.java)

        journalpostService.opprettJournalpost(PunsjJournalpost(UUID.randomUUID(), journalpostId = melding.journalpostId, aktørId = melding.aktørId))

        aksjonspunktRepository.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = UUID.randomUUID().toString(),
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        val hentAlleAksjonspunkter = aksjonspunktRepository.hentAlleAksjonspunkter(melding.journalpostId)
        assertThat(hentAlleAksjonspunkter).hasSize(1)

        val person = personRepository.lagre("2123245", morsAktørId)
        val mappeId = mappeRepository.opprettEllerHentMappeForPerson(person.personId)
        val bunkeId = bunkeRepository.opprettEllerHentBunkeForFagsakType(mappeId, PunsjFagsakYtelseType.PLEIEPENGER_SYKT_BARN)
        søknadRepository.opprettSøknad(SøknadEntitet(søknadId, bunkeId, person.personId))

        Mockito.doNothing().`when`(hendelseProducer).sendMedOnSuccess(topicName = captureString(topicCaptor), data = captureString(valueCaptor), key = captureString(keyCaptor), onSuccess = captureFun(anyCaptor))
        aksjonspunktService.settPåVentOgSendTilLos(melding.journalpostId, søknadId)

        val value = valueCaptor.value
        val verdiFraKafka = objectMapper().readValue<PunsjEventDto>(value)
        assertThat(verdiFraKafka.aksjonspunktKoderMedStatusListe).isEqualTo(mutableMapOf(Pair("PUNSJ", "UTFO"), Pair("MER_INFORMASJON", "OPPR")))
        assertThat(verdiFraKafka.aktørId).isEqualTo(morsAktørId)
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
