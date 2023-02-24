package no.nav.k9punsj.fordel

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.metrikker.Metrikk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HendelseMottaker @Autowired constructor(
    private val journalpostService: JournalpostService,
    private val aksjonspunktService: AksjonspunktService,
    private val meterRegistry: MeterRegistry
) {
    private companion object {
        private val log: Logger = LoggerFactory.getLogger(HendelseMottaker::class.java)
    }

    suspend fun prosesser(fordelPunsjEventDto: FordelPunsjEventDto) {
        val journalpostId = fordelPunsjEventDto.journalpostId
        val journalpostIkkeEksisterer = journalpostService.journalpostIkkeEksisterer(journalpostId)

        if (journalpostIkkeEksisterer) {
            val aktørId = fordelPunsjEventDto.aktørId
            val punsjEventType = PunsjInnsendingType.fraKode(fordelPunsjEventDto.type).kode
            val ytelse = FagsakYtelseType.fromKode(fordelPunsjEventDto.ytelse).kode
            val gosysoppgaveId = fordelPunsjEventDto.gosysoppgaveId

            // TODO: Dersom SKRIV_TIL_OSS_SPØRMSÅL, SKRIV_TIL_OSS_SVAR, SAMTALEREFERAT
            publiserJournalpostMetrikk(fordelPunsjEventDto)

            val uuid = UUID.randomUUID()
            val punsjJournalpost = PunsjJournalpost(
                uuid = uuid,
                journalpostId = journalpostId,
                aktørId = aktørId,
                ytelse = ytelse,
                type = punsjEventType,
                gosysoppgaveId = gosysoppgaveId
            )
            journalpostService.opprettJournalpost(punsjJournalpost)
            aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
                punsjJournalpost = punsjJournalpost,
                aksjonspunkt = Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET),
                type = punsjEventType,
                ytelse = fordelPunsjEventDto.ytelse
            )
        } else {
            if (PunsjInnsendingType.fraKode(fordelPunsjEventDto.type) == PunsjInnsendingType.PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG) {
                val journalpostFraDb = journalpostService.hent(journalpostId)
                if (journalpostFraDb.type != null && PunsjInnsendingType.fraKode(journalpostFraDb.type) != PunsjInnsendingType.PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG) {
                    journalpostService.settInnsendingstype(PunsjInnsendingType.PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG, journalpostId)
                    aksjonspunktService.settUtførtPåAltSendLukkOppgaveTilK9Los(journalpostId, false, null)
                } else {
                    log.info("Journalposten($journalpostId) kjenner punsj fra før, blir ikke laget ny oppgave")
                }
            } else {
                log.info("Journalposten($journalpostId) kjenner punsj fra før, blir ikke laget ny oppgave")
            }
        }
    }

    private fun publiserJournalpostMetrikk(fordelPunsjEventDto: FordelPunsjEventDto) {
        meterRegistry.counter(
            Metrikk.ANTALL_OPPRETTET_JOURNALPOST_COUNTER.navn,
            listOf(
                Tag.of("ytelsestype", fordelPunsjEventDto.ytelse),
                Tag.of("punsjInnsendingstype", fordelPunsjEventDto.type)
            )
        ).increment()
    }
}
