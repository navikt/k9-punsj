package no.nav.k9punsj.akjonspunkter

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.fordel.FordelPunsjEventDto
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.util.DatabaseUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@ExtendWith(SpringExtension::class)
internal class AksjonspunktRepositoryTest {

    @Test
    fun `skal overskrive aksjonspunkt hvis det matcher på journalpostId + kode og status`(): Unit = runBlocking {
        val journalpostRepo = DatabaseUtil.getJournalpostRepo()
        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666", type = "test", ytelse = "test")

        journalpostRepo.opprettJournalpost(PunsjJournalpost(UUID.randomUUID(), journalpostId = melding.journalpostId, aktørId = melding.aktørId))

        val aksjonspunktRepo = DatabaseUtil.getAksjonspunktRepo()

        aksjonspunktRepo.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = UUID.randomUUID().toString(),
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        val hentAlleAksjonspunkter = aksjonspunktRepo.hentAlleAksjonspunkter(melding.journalpostId)
        Assertions.assertThat(hentAlleAksjonspunkter).hasSize(1)

        aksjonspunktRepo.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = UUID.randomUUID().toString(),
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        val hentAlleAksjonspunkter2 = aksjonspunktRepo.hentAlleAksjonspunkter(melding.journalpostId)
        Assertions.assertThat(hentAlleAksjonspunkter2).hasSize(1)
    }

    @Test
    fun `skal kunne opprette ny aksjonspunkt med samme kode hvis det ligger et der fra før med annen status`(): Unit = runBlocking {
        val journalpostRepo = DatabaseUtil.getJournalpostRepo()
        val melding = FordelPunsjEventDto(aktørId = "1234567891", journalpostId = "667", type = "test", ytelse = "test")

        journalpostRepo.opprettJournalpost(PunsjJournalpost(UUID.randomUUID(), journalpostId = melding.journalpostId, aktørId = melding.aktørId))

        val aksjonspunktRepo = DatabaseUtil.getAksjonspunktRepo()

        val aksjonspunktId = UUID.randomUUID().toString()
        aksjonspunktRepo.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = aksjonspunktId,
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        aksjonspunktRepo.settStatus(aksjonspunktId, AksjonspunktStatus.UTFØRT)

        val hentAlleAksjonspunkter = aksjonspunktRepo.hentAlleAksjonspunkter(melding.journalpostId)
        Assertions.assertThat(hentAlleAksjonspunkter).hasSize(1)

        aksjonspunktRepo.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = UUID.randomUUID().toString(),
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        val hentAlleAksjonspunkter2 = aksjonspunktRepo.hentAlleAksjonspunkter(melding.journalpostId)
        Assertions.assertThat(hentAlleAksjonspunkter2).hasSize(2)
    }
}
