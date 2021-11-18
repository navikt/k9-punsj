package no.nav.k9punsj.brev

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.util.IdGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@ExtendWith(SpringExtension::class)
internal class BrevRepositoryTest {

    @Test
    fun `Skal lagre brev`(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val journalpostRepository = DatabaseUtil.getJournalpostRepo()

        val journalpost1 =
            Journalpost(uuid = UUID.randomUUID(), journalpostId = IdGenerator.nesteId(), aktørId = dummyAktørId, type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(journalpost1) {
            journalpost1
        }

        val repo = DatabaseUtil.getBrevRepo()
        val forJournalpostId = journalpost1.journalpostId
        val brev = BrevEntitet(BrevId().nyId(), forJournalpostId, BrevData("Statnett"), BrevType.FRITEKSTBREV)

        repo.opprettBrev(brev = brev)
        val alleBrev : List<BrevEntitet> = repo.hentAlleBrevPåJournalpost(forJournalpostId)

        assertThat(alleBrev).hasSize(1)
        assertThat(alleBrev[0].brevData.mottaker).isEqualTo("Statnett")
    }
}

