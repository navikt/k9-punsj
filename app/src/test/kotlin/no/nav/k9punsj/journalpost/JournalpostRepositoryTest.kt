package no.nav.k9punsj.journalpost

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.awaitExchangeBlocking
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@ExtendWith(SpringExtension::class, MockKExtension::class)
@TestPropertySource(locations = ["classpath:application.yml"])
internal class JournalpostRepositoryTest {

    val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"


    @Test
    fun `Skal finne alle journalposter på personen`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost1 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(journalpost1) {
            journalpost1
        }

        val journalpost2 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(journalpost2) {
            journalpost2
        }

        val hent = journalpostRepository.hent(journalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(journalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(2)
        assertThat(finnJournalposterPåPerson[0].type).isEqualTo(PunsjInnsendingType.PAPIRSØKNAD.kode)
    }

    @Test
    fun `Skal bare finne de fra fordel`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost1 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(journalpost1, KildeType.SAKSBEHANDLER) {
            journalpost1
        }

        val journalpost2 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(journalpost2, KildeType.FORDEL) {
            journalpost2
        }

        val hent = journalpostRepository.hent(journalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(journalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPersonBareFordel(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(1)
        assertThat(finnJournalposterPåPerson[0].type).isEqualTo(PunsjInnsendingType.PAPIRSØKNAD.kode)
    }

    @Test
    fun `Skal sette status til ferdig`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost1 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost1) {
            journalpost1
        }

        val journalpost2 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost2) {
            journalpost2
        }

        val hent = journalpostRepository.hent(journalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(journalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(2)

        journalpostRepository.settAlleTilFerdigBehandlet(listOf(journalpost1.journalpostId, journalpost2.journalpostId))

        val finnJournalposterPåPersonSkalGiTom = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPersonSkalGiTom).isEmpty()
    }

    @Test
    fun `Endepunkt brukt for resett av journalpost fungerer`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost1 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, skalTilK9 = false)
        journalpostRepository.lagre(journalpost1) {
            journalpost1
        }

        val res =
            client.get().uri {
                it.pathSegment("api", "journalpost", "resett", journalpost1.journalpostId).build()
            }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

        Assertions.assertEquals(HttpStatus.OK, res.statusCode())

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)

        Assertions.assertEquals(finnJournalposterPåPerson[0].skalTilK9, null)
    }

    @Test
    fun `Skal sjekke om punsj kan sende inn`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost1 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost1) {
            journalpost1
        }

        val journalpost2 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost2) {
            journalpost2
        }

        val hent = journalpostRepository.hent(journalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(journalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(2)

        val kanSendeInn =
            journalpostRepository.kanSendeInn(listOf(journalpost1.journalpostId, journalpost2.journalpostId))
        assertThat(kanSendeInn).isTrue

        journalpostRepository.settAlleTilFerdigBehandlet(listOf(journalpost1.journalpostId, journalpost2.journalpostId))

        val kanSendeInn2 =
            journalpostRepository.kanSendeInn(listOf(journalpost1.journalpostId, journalpost2.journalpostId))

        assertThat(kanSendeInn2).isFalse
    }

    @Test
    fun `skal sette kilde hvis journalposten ikke finnes i databasen fra før`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost2 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost2) {
            journalpost2
        }

        assertThat(journalpostRepository.finnJournalposterPåPerson(dummyAktørId)).hasSize(1)
        val journalpost1 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)

        journalpostRepository.settKildeHvisIkkeFinnesFraFør(listOf(journalpost1.journalpostId, journalpost2.journalpostId), dummyAktørId)

        assertThat(journalpostRepository.finnJournalposterPåPerson(dummyAktørId)).hasSize(2)
    }

    @Test
    fun `skal vise om journalposten må til infotrygd`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost2 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, skalTilK9 = false)
        journalpostRepository.lagre(journalpost2) {
            journalpost2
        }

        assertThat(journalpostRepository.hent(journalpost2.journalpostId).skalTilK9).isFalse()
    }

    @Test
    fun `skal kunne sette alle til ferdig`(): Unit = runBlocking  {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost1 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost1) {
            journalpost1
        }

        val journalpost2 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost2) {
            journalpost2
        }

        journalpostRepository.settAlleTilFerdigBehandlet(listOf(journalpost1.journalpostId, journalpost2.journalpostId))
        assertThat(journalpostRepository.kanSendeInn(listOf(journalpost1.journalpostId, journalpost2.journalpostId))).isFalse()
    }

    @Test
    fun `skal feil hvis ikke alle kan settes til ferdig`(): Unit = runBlocking  {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost1 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost1) {
            journalpost1
        }

        val journalpost2 = Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)

        var harFåttEx = false
        try {
            journalpostRepository.settAlleTilFerdigBehandlet(listOf(journalpost1.journalpostId,
                journalpost2.journalpostId))
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo("Klarte ikke sette alle til ferdig")
            harFåttEx = true
        }
        assertThat(harFåttEx).isTrue()
    }
}

