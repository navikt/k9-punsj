package no.nav.k9punsj.fordel

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.TestBeans
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktServiceImpl
import no.nav.k9punsj.db.repository.SøknadRepository
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID


@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@ContextConfiguration(classes = [
    TestBeans::class,
    HendelseMottaker::class,
    AksjonspunktServiceImpl::class,
    JournalpostRepository::class,
    AksjonspunktRepository::class,
    SøknadRepository::class
])
internal class HendelseMottakerTest {
    @Autowired
    private lateinit var hendelseMottaker: HendelseMottaker

    @Test
    fun `skal lagre ned informasjon om journalpost`() : Unit = runBlocking {
        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666", type = PunsjInnsendingType.PAPIRSØKNAD.kode, ytelse = "PSB")
        hendelseMottaker.prosesser(melding)

        val journalpost = hendelseMottaker.journalpostRepository.hent("666")
        Assertions.assertThat(journalpost).isNotNull
    }

    @Test
    fun `skal ikke lagre ned informasjon om journalpost når det kommer samme uten status`() : Unit = runBlocking {
        val journalpostRepository = hendelseMottaker.journalpostRepository
        val meldingSomIkkeSkalBrukes = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666", type = PunsjInnsendingType.PAPIRSØKNAD.kode, ytelse = "PSB")

        val journalpostTilDb = Journalpost(UUID.randomUUID(), journalpostId = meldingSomIkkeSkalBrukes.journalpostId, aktørId = meldingSomIkkeSkalBrukes.aktørId, type = PunsjInnsendingType.DIGITAL_ETTERSENDELSE.kode)
        journalpostRepository.lagre(journalpostTilDb){
            journalpostTilDb
        }

        hendelseMottaker.prosesser(meldingSomIkkeSkalBrukes)

        val journalpost = journalpostRepository.hent("666")
        Assertions.assertThat(journalpost.type).isEqualTo(PunsjInnsendingType.DIGITAL_ETTERSENDELSE.kode)
    }

    @Test
    fun `skal ikke lagre ned informasjon om journalpost når journalposten har ankommet fra før med samme status`() : Unit = runBlocking {
        val journalpostRepository = hendelseMottaker.journalpostRepository
        val meldingSomIkkeSkalBrukes = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666", type = PunsjInnsendingType.PAPIRSØKNAD.kode, ytelse = "PSB", FordelStatusType.OPPRETTET.kode)

        val journalpostTilDb = Journalpost(UUID.randomUUID(), journalpostId = meldingSomIkkeSkalBrukes.journalpostId, aktørId = meldingSomIkkeSkalBrukes.aktørId, type = PunsjInnsendingType.DIGITAL_ETTERSENDELSE.kode, fordelStatusType = FordelStatusType.OPPRETTET.kode)
        journalpostRepository.lagre(journalpostTilDb){
            journalpostTilDb
        }

        hendelseMottaker.prosesser(meldingSomIkkeSkalBrukes)

        val journalpost = journalpostRepository.hent("666")
        Assertions.assertThat(journalpost.type).isEqualTo(PunsjInnsendingType.DIGITAL_ETTERSENDELSE.kode)
    }

    @Test
    fun `skal lagre ned informasjon om journalpost når journalposten har ankommet fra før med ny status`() : Unit = runBlocking {
        val journalpostRepository = hendelseMottaker.journalpostRepository
        val meldingSomSkalBrukes = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666", type = PunsjInnsendingType.PAPIRSØKNAD.kode, ytelse = "PSB", fordelStatus = FordelStatusType.LUKKET_FRA_FORDEL.kode)

        val journalpostTilDb = Journalpost(UUID.randomUUID(), journalpostId = meldingSomSkalBrukes.journalpostId, aktørId = meldingSomSkalBrukes.aktørId, type = PunsjInnsendingType.DIGITAL_ETTERSENDELSE.kode, fordelStatusType = FordelStatusType.OPPRETTET.kode)
        journalpostRepository.lagre(journalpostTilDb){
            journalpostTilDb
        }

        hendelseMottaker.prosesser(meldingSomSkalBrukes)

        val journalpost = journalpostRepository.hent("666")
        Assertions.assertThat(journalpost.type).isEqualTo(PunsjInnsendingType.PAPIRSØKNAD.kode)
    }
}
