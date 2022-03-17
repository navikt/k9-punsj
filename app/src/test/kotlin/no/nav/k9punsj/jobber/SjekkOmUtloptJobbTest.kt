package no.nav.k9punsj.jobber

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.akjonspunkter.AksjonspunktEntitet
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.akjonspunkter.VentÅrsakType
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.util.DatabaseUtil.Companion.getAksjonspunktRepo
import no.nav.k9punsj.util.DatabaseUtil.Companion.getJournalpostRepo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class SjekkOmUtloptJobbTest {

    @MockBean
    lateinit var hendelseProducer: HendelseProducer

    @Test
    fun `Skal finne alle aksjonspunkter som har utløpt og sende oppgaver på disse`(): Unit = runBlocking {
        // Arrange
        val aksjonspunktRepository = getAksjonspunktRepo()
        val journalpostRepository = getJournalpostRepo()

        val sjekkOmUtloptJobb = SjekkOmUtloptJobb(aksjonspunktRepository, hendelseProducer, journalpostRepository)

        val dummyAktørId = "1000000000000"

        val punsjJournalpost = PunsjJournalpost(uuid = UUID.randomUUID(), journalpostId = "466988237", aktørId = dummyAktørId)
        journalpostRepository.lagre(punsjJournalpost) {
            punsjJournalpost
        }

        val aksjonspunktId = UUID.randomUUID().toString()
        aksjonspunktRepository.opprettAksjonspunkt(AksjonspunktEntitet(aksjonspunktId,
            AksjonspunktKode.VENTER_PÅ_INFORMASJON,
            punsjJournalpost.journalpostId,
            AksjonspunktStatus.OPPRETTET,
            LocalDateTime.now().minusDays(1),
            VentÅrsakType.VENT_TRENGER_FLERE_OPPLYSINGER))

        // Act
        sjekkOmUtloptJobb.sjekkeOmAksjonspunktHarLøptUt()

        // Assert
        val hentAlleAksjonspunkter = aksjonspunktRepository.hentAlleAksjonspunkter(punsjJournalpost.journalpostId)
        assertThat(hentAlleAksjonspunkter).hasSize(2)

        val venterPåInformasjon =
            hentAlleAksjonspunkter.first { it.aksjonspunktKode == AksjonspunktKode.VENTER_PÅ_INFORMASJON }
        assertThat(venterPåInformasjon.aksjonspunktStatus).isEqualTo(AksjonspunktStatus.UTFØRT)

        val harUtløpt = hentAlleAksjonspunkter.first { it.aksjonspunktKode == AksjonspunktKode.PUNSJ_HAR_UTLØPT }
        assertThat(harUtløpt.aksjonspunktStatus).isEqualTo(AksjonspunktStatus.OPPRETTET)
    }
}
