package no.nav.k9punsj.akjonspunkter

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.fordel.FordelPunsjEventDto
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

internal class AksjonspunktRepositoryTest: AbstractContainerBaseTest() {

    @Autowired
    lateinit var journalpostRepository : JournalpostRepository

    @Autowired
    lateinit var aksjonspunktRepository : AksjonspunktRepository

    @BeforeEach
    fun setUp() {
        cleanUpDB()
    }

    @Test
    fun `skal overskrive aksjonspunkt hvis det matcher på journalpostId + kode og status`(): Unit = runBlocking {

        val melding = FordelPunsjEventDto(aktørId = "1234567890", journalpostId = "666", type = "test", ytelse = "test")

        journalpostRepository.opprettJournalpost(PunsjJournalpost(UUID.randomUUID(), journalpostId = melding.journalpostId, aktørId = melding.aktørId))

        aksjonspunktRepository.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = UUID.randomUUID().toString(),
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        val hentAlleAksjonspunkter = aksjonspunktRepository.hentAlleAksjonspunkter(melding.journalpostId)
        Assertions.assertThat(hentAlleAksjonspunkter).hasSize(1)

        aksjonspunktRepository.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = UUID.randomUUID().toString(),
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        val hentAlleAksjonspunkter2 = aksjonspunktRepository.hentAlleAksjonspunkter(melding.journalpostId)
        Assertions.assertThat(hentAlleAksjonspunkter2).hasSize(1)
    }

    @Test
    fun `skal kunne opprette ny aksjonspunkt med samme kode hvis det ligger et der fra før med annen status`(): Unit = runBlocking {
        val melding = FordelPunsjEventDto(aktørId = "1234567891", journalpostId = "667", type = "test", ytelse = "test")

        journalpostRepository.opprettJournalpost(PunsjJournalpost(UUID.randomUUID(), journalpostId = melding.journalpostId, aktørId = melding.aktørId))

        val aksjonspunktId = UUID.randomUUID().toString()
        aksjonspunktRepository.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = aksjonspunktId,
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        aksjonspunktRepository.settStatus(aksjonspunktId, AksjonspunktStatus.UTFØRT)

        val hentAlleAksjonspunkter = aksjonspunktRepository.hentAlleAksjonspunkter(melding.journalpostId)
        Assertions.assertThat(hentAlleAksjonspunkter).hasSize(1)

        aksjonspunktRepository.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId = UUID.randomUUID().toString(),
                aksjonspunktKode = AksjonspunktKode.PUNSJ,
                journalpostId = melding.journalpostId,
                aksjonspunktStatus = AksjonspunktStatus.OPPRETTET
            )
        )

        val hentAlleAksjonspunkter2 = aksjonspunktRepository.hentAlleAksjonspunkter(melding.journalpostId)
        Assertions.assertThat(hentAlleAksjonspunkter2).hasSize(2)
    }
}
