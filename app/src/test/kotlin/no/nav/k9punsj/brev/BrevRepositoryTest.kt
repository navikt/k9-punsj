package no.nav.k9punsj.brev

import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9.formidling.kontrakt.kodeverk.IdType
import no.nav.k9punsj.brev.dto.BrevType
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.journalpost.PunsjJournalpost
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

        val punsjJournalpost1 =
            PunsjJournalpost(uuid = UUID.randomUUID(),
                journalpostId = IdGenerator.nesteId(),
                aktørId = dummyAktørId,
                type = PunsjInnsendingType.PAPIRSØKNAD.kode)
        journalpostRepository.lagre(punsjJournalpost1) {
            punsjJournalpost1
        }

        val repo = DatabaseUtil.getBrevRepo()
        val forJournalpostId = punsjJournalpost1.journalpostId
        val brevData = DokumentbestillingDto("1",
            "2",
            "123",
            "1234",
            DokumentbestillingDto.Mottaker(IdType.ORGNR.name, "Statnett"),
            FagsakYtelseType.OMSORGSPENGER,
            "2")
        val brev = BrevEntitet(
            forJournalpostId = forJournalpostId,
            brevData = brevData,
            brevType = BrevType.FRITEKSTBREV
        )

        val saksbehandler = "saksbehandler"
        repo.opprettBrev(brev = brev, saksbehandler = saksbehandler)
        val alleBrev: List<BrevEntitet> = repo.hentAlleBrevPåJournalpost(forJournalpostId)

        assertThat(alleBrev).hasSize(1)
        val brevEntitet = alleBrev[0]
        assertThat(brevEntitet.brevData.mottaker.id).isEqualTo("Statnett")
        assertThat(brevEntitet.opprettet_av).isEqualTo(saksbehandler)
        assertThat(brevEntitet.opprettet_tid).isNotNull()
    }
}

