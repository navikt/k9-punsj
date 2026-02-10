package no.nav.k9punsj.jobber

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.akjonspunkter.AksjonspunktEntitet
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.akjonspunkter.VentÅrsakType
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.kafka.HendelseProducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime
import java.util.*

internal class SjekkOmUtløptJobbTest : AbstractContainerBaseTest() {

    @MockitoBean
    lateinit var hendelseProducer: HendelseProducer

    @Autowired
    lateinit var aksonspunktRepository: AksjonspunktRepository

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository


    @Test
    fun `Skal finne alle aksjonspunkter som har utløpt og sende oppgaver på disse`(): Unit = runBlocking {

        val sjekkOmUtløptJobb = SjekkOmUtløptJobb(
            aksjonspunktRepository = aksonspunktRepository,
            hendelseProducer = hendelseProducer,
            journalpostRepository = journalpostRepository,
            k9losAksjonspunkthendelseTopic = "test",
            k9PunsjTilLosTopic = "test"
        )

        val dummyAktørId = "1000000000000"

        val punsjJournalpost = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = "466988237",
            aktørId = dummyAktørId
        )

        journalpostRepository.lagre(punsjJournalpost) {
            punsjJournalpost
        }

        val aksjonspunktId = UUID.randomUUID().toString()
        aksonspunktRepository.opprettAksjonspunkt(
            AksjonspunktEntitet(
                aksjonspunktId,
                AksjonspunktKode.VENTER_PÅ_INFORMASJON,
                punsjJournalpost.journalpostId,
                AksjonspunktStatus.OPPRETTET,
                LocalDateTime.now().minusDays(1),
                VentÅrsakType.VENT_TRENGER_FLERE_OPPLYSINGER
            )
        )

        // Act
        sjekkOmUtløptJobb.sjekkeOmAksjonspunktHarLøptUt()

        // Assert
        val hentAlleAksjonspunkter = aksonspunktRepository.hentAlleAksjonspunkter(punsjJournalpost.journalpostId)
        assertThat(hentAlleAksjonspunkter).hasSize(2)

        val venterPåInformasjon =
            hentAlleAksjonspunkter.first { it.aksjonspunktKode == AksjonspunktKode.VENTER_PÅ_INFORMASJON }
        assertThat(venterPåInformasjon.aksjonspunktStatus).isEqualTo(AksjonspunktStatus.UTFØRT)

        val harUtløpt = hentAlleAksjonspunkter.first { it.aksjonspunktKode == AksjonspunktKode.PUNSJ_HAR_UTLØPT }
        assertThat(harUtløpt.aksjonspunktStatus).isEqualTo(AksjonspunktStatus.OPPRETTET)
    }
}
