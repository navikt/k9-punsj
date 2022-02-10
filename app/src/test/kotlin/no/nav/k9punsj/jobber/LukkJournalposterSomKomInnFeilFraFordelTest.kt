package no.nav.k9punsj.jobber

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktServiceImpl
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.db.repository.BunkeRepository
import no.nav.k9punsj.db.repository.MappeRepository
import no.nav.k9punsj.db.repository.PersonRepository
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.rest.eksternt.pdl.TestPdlService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [
    AksjonspunktServiceImpl::class,
    JournalpostRepository::class,
    AksjonspunktRepository::class,
    MappeRepository::class,
    BunkeRepository::class,
    SøknadRepository::class,
    SøknadRepository::class,
    PersonService::class,
    PersonRepository::class,
    TestPdlService::class,
    TestBeans::class
])
internal class LukkJournalposterSomKomInnFeilFraFordelTest {

    @Autowired
    private lateinit var aksjonspunktService: AksjonspunktServiceImpl

    @MockBean
    private lateinit var hendelseProducer: HendelseProducer

    @Autowired
    private lateinit var journalpostRepository: JournalpostRepository


    @Test
    fun `skal test jobben`(): Unit = runBlocking {
        val jp = Journalpost(UUID.randomUUID(), journalpostId = "545426642", aktørId = "23423434")
        journalpostRepository.opprettJournalpost(jp)
        aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(jp, AksjonspunktKode.PUNSJ to AksjonspunktStatus.OPPRETTET, null, null)

        val jp1 = Journalpost(UUID.randomUUID(), journalpostId = "545241256", aktørId = "23423434")
        journalpostRepository.opprettJournalpost(jp1)
        aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(jp1, AksjonspunktKode.PUNSJ to AksjonspunktStatus.OPPRETTET, null, null)

        val jobben = LukkJournalposterSomKomInnFeilFraFordel(aksjonspunktService, journalpostRepository)

        jobben.sjekkeOmAksjonspunktHarLøptUt()

        val res = journalpostRepository.kanSendeInn(listOf("545426642"))
        val res2 = journalpostRepository.kanSendeInn(listOf("545241256"))
        Assertions.assertThat(res).isFalse
        Assertions.assertThat(res2).isFalse
    }
}
