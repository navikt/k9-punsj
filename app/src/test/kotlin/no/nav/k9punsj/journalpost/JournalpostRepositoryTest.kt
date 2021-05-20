package no.nav.k9punsj.journalpost

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@ExtendWith(SpringExtension::class)
internal class JournalpostRepositoryTest {

    @Test
    fun `Skal finne alle journalposter på personen`(): Unit = runBlocking {
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

        journalpostRepository.settBehandletFerdig(mutableSetOf(journalpost1.journalpostId, journalpost2.journalpostId))

        val finnJournalposterPåPersonSkalGiTom = journalpostRepository.finnJournalposterPåPerson(dummyAktørId)
        assertThat(finnJournalposterPåPersonSkalGiTom).isEmpty()
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

        journalpostRepository.settBehandletFerdig(mutableSetOf(journalpost1.journalpostId, journalpost2.journalpostId))

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
}

