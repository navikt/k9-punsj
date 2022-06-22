package no.nav.k9punsj.journalpost

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.felles.PunsjJournalpostKildeType
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.WebClientUtils.awaitExchangeBlocking
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
internal class PunsjJournalpostRepositoryTest {

    val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

    @Test
    fun `Skal finne alle journalposter på personen`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        val hent = journalpostRepository.hent(punsjJournalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(punsjJournalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(2)
        assertThat(finnJournalposterPåPerson[0].type).isEqualTo(PunsjInnsendingType.PAPIRSØKNAD.kode)
    }

    @Test
    fun `Skal bare finne de fra fordel`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(punsjJournalpost1, PunsjJournalpostKildeType.SAKSBEHANDLER) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(punsjJournalpost2, PunsjJournalpostKildeType.FORDEL) {
            punsjJournalpost2
        }

        val hent = journalpostRepository.hent(punsjJournalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(punsjJournalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPersonBareFordel(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(1)
        assertThat(finnJournalposterPåPerson[0].type).isEqualTo(PunsjInnsendingType.PAPIRSØKNAD.kode)
    }

    @Test
    fun `Skal sette status til ferdig`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        val hent = journalpostRepository.hent(punsjJournalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(punsjJournalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(2)

        journalpostRepository.settAlleTilFerdigBehandlet(listOf(punsjJournalpost1.journalpostId, punsjJournalpost2.journalpostId))

        val finnJournalposterPåPersonSkalGiTom = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPersonSkalGiTom).isEmpty()
    }

    @Test
    fun `Endepunkt brukt for resett av journalpost fungerer`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, skalTilK9 = false)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val res =
            client.get().uri {
                it.pathSegment("api", "journalpost", "resett", punsjJournalpost1.journalpostId).build()
            }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

        Assertions.assertEquals(HttpStatus.OK, res.statusCode())

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)

        Assertions.assertEquals(finnJournalposterPåPerson[0].skalTilK9, null)
    }

    @Test
    fun `Skal sjekke om punsj kan sende inn`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        val hent = journalpostRepository.hent(punsjJournalpost1.journalpostId)
        assertThat(hent.aktørId!!).isEqualTo(dummyAktørId)

        val hent2 = journalpostRepository.hent(punsjJournalpost2.journalpostId)
        assertThat(hent2.aktørId!!).isEqualTo(dummyAktørId)

        val finnJournalposterPåPerson = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPerson).hasSize(2)

        val kanSendeInn =
            journalpostRepository.kanSendeInn(listOf(punsjJournalpost1.journalpostId, punsjJournalpost2.journalpostId))
        assertThat(kanSendeInn).isTrue

        journalpostRepository.settAlleTilFerdigBehandlet(listOf(punsjJournalpost1.journalpostId, punsjJournalpost2.journalpostId))

        val kanSendeInn2 =
            journalpostRepository.kanSendeInn(listOf(punsjJournalpost1.journalpostId, punsjJournalpost2.journalpostId))

        assertThat(kanSendeInn2).isFalse
    }

    @Test
    fun `skal sette kilde hvis journalposten ikke finnes i databasen fra før`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        assertThat(journalpostRepository.finnJournalposterPåPerson(dummyAktørId)).hasSize(1)
        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)

        journalpostRepository.settKildeHvisIkkeFinnesFraFør(listOf(punsjJournalpost1.journalpostId, punsjJournalpost2.journalpostId), dummyAktørId)

        assertThat(journalpostRepository.finnJournalposterPåPerson(dummyAktørId)).hasSize(2)
    }

    @Test
    fun `skal vise om journalposten må til infotrygd`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, skalTilK9 = false)
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        assertThat(journalpostRepository.hent(punsjJournalpost2.journalpostId).skalTilK9).isFalse()
    }

    @Test
    fun `skal kunne sette alle til ferdig`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost2) {
            punsjJournalpost2
        }

        journalpostRepository.settAlleTilFerdigBehandlet(listOf(punsjJournalpost1.journalpostId, punsjJournalpost2.journalpostId))
        assertThat(journalpostRepository.kanSendeInn(listOf(punsjJournalpost1.journalpostId, punsjJournalpost2.journalpostId))).isFalse()
    }

    @Test
    fun `skal feil hvis ikke alle kan settes til ferdig`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val punsjJournalpost2 = PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId)

        var harFåttEx = false
        try {
            journalpostRepository.settAlleTilFerdigBehandlet(
                listOf(
                    punsjJournalpost1.journalpostId,
                    punsjJournalpost2.journalpostId
                )
            )
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo("Klarte ikke sette alle til ferdig")
            harFåttEx = true
        }
        assertThat(harFåttEx).isTrue()
    }
}
