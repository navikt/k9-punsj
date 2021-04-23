package no.nav.k9punsj.journalpost

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.util.DatabaseUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@ExtendWith(SpringExtension::class)
internal class JournalpostRepositoryTest {

    @Test
    fun `Skal finne alle journalposter på personen`(): Unit = runBlocking {
        val dummyAktørId = "1000000000000"
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost1 = Journalpost(uuid = UUID.randomUUID(), journalpostId = "466988237", aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost1) {
            journalpost1
        }

        val journalpost2 = Journalpost(uuid = UUID.randomUUID(), journalpostId = "466988234", aktørId = dummyAktørId)
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
}

