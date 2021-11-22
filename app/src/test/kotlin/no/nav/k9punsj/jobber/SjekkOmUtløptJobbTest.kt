package no.nav.k9punsj.jobber

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.akjonspunkter.AksjonspunktEntitet
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.akjonspunkter.VentÅrsakType
import no.nav.k9punsj.journalpost.Journalpost
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
internal class SjekkOmUtløptJobbTest {

    @MockBean
    lateinit var hendelseProducer: HendelseProducer

    @Test
    fun `Skal finne alle aksjonspunkter som har utløpt og sende oppgaver på disse`(): Unit = runBlocking {
        // Arrange
        val aksjonspunktRepository = getAksjonspunktRepo()
        val journalpostRepository = getJournalpostRepo()

        val sjekkOmUtløptJobb = SjekkOmUtløptJobb(aksjonspunktRepository, hendelseProducer, journalpostRepository, "privat-k9punsj-aksjonspunkthendelse-v1")

        val dummyAktørId = "1000000000000"

        val journalpost = Journalpost(uuid = UUID.randomUUID(), journalpostId = "466988237", aktørId = dummyAktørId)
        journalpostRepository.lagre(journalpost) {
            journalpost
        }

        val aksjonspunktId = UUID.randomUUID().toString()
        aksjonspunktRepository.opprettAksjonspunkt(AksjonspunktEntitet(aksjonspunktId,
            AksjonspunktKode.VENTER_PÅ_INFORMASJON,
            journalpost.journalpostId,
            AksjonspunktStatus.OPPRETTET,
            LocalDateTime.now().minusDays(1),
            VentÅrsakType.VENT_TRENGER_FLERE_OPPLYSINGER))

        // Act
        sjekkOmUtløptJobb.sjekkeOmAksjonspunktHarLøptUt()

        // Assert
        val hentAlleAksjonspunkter = aksjonspunktRepository.hentAlleAksjonspunkter(journalpost.journalpostId)
        assertThat(hentAlleAksjonspunkter).hasSize(2)

        val venterPåInformasjon =
            hentAlleAksjonspunkter.first { it.aksjonspunktKode == AksjonspunktKode.VENTER_PÅ_INFORMASJON }
        assertThat(venterPåInformasjon.aksjonspunktStatus).isEqualTo(AksjonspunktStatus.UTFØRT)

        val harUtløpt = hentAlleAksjonspunkter.first { it.aksjonspunktKode == AksjonspunktKode.PUNSJ_HAR_UTLØPT }
        assertThat(harUtløpt.aksjonspunktStatus).isEqualTo(AksjonspunktStatus.OPPRETTET)
    }
}
