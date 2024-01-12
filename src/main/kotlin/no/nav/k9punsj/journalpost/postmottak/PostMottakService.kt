package no.nav.k9punsj.journalpost.postmottak

import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.JournalpostInfo
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PostMottakService(
    private val journalpostService: JournalpostService,
    private val pdlService: PdlService,
    private val aksjonspunktService: AksjonspunktService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(PostMottakService::class.java)
    }

    suspend fun klassifiserOgJournalfør(mottattJournalpost: JournalpostMottaksHaandteringDto) {
        val oppdatertJournalpost = hentOgOppdaterJournalpostFraDB(mottattJournalpost)
        val safJournalpostinfo = hentJournalpostInfoFraSaf(oppdatertJournalpost)

        // TODO: Hent og reserver saksnummer fra k9-sak?

        if (!erFerdigstiltEllerJournalført(safJournalpostinfo) && harSaksnummer(mottattJournalpost)) {
            oppdaterOgFerdigstillJournalpostMedSaksnummer(mottattJournalpost, oppdatertJournalpost)
        }

        lagreTilDB(oppdatertJournalpost)
        opprettAksjonspunktOgSendTilK9Los(oppdatertJournalpost, mottattJournalpost)
    }

    private fun harSaksnummer(mottattJournalpost: JournalpostMottaksHaandteringDto) =
        mottattJournalpost.saksnummer != null

    private suspend fun opprettAksjonspunktOgSendTilK9Los(
        oppdatertJournalpost: PunsjJournalpost,
        mottattJournalpost: JournalpostMottaksHaandteringDto,
    ) {
        aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
            punsjJournalpost = oppdatertJournalpost,
            aksjonspunkt = Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET),
            type = oppdatertJournalpost.type,
            ytelse = mottattJournalpost.fagsakYtelseTypeKode
        )
    }

    private suspend fun oppdaterOgFerdigstillJournalpostMedSaksnummer(
        mottattJournalpost: JournalpostMottaksHaandteringDto,
        oppdatertJournalpost: PunsjJournalpost,
    ) {
        journalpostService.oppdaterOgFerdigstillForMottak(mottattJournalpost)
        logger.info("Ferdigstilt journalpost: ${oppdatertJournalpost.journalpostId}")
    }

    private suspend fun lagreTilDB(oppdatertJournalpost: PunsjJournalpost) {
        journalpostService.lagre(punsjJournalpost = oppdatertJournalpost)
    }

    private fun erFerdigstiltEllerJournalført(safJournalpostinfo: JournalpostInfo?): Boolean {
        val erFerdigstiltEllerJournalfoert = (
                safJournalpostinfo?.journalpostStatus == SafDtos.Journalstatus.FERDIGSTILT.name ||
                        safJournalpostinfo?.journalpostStatus == SafDtos.Journalstatus.JOURNALFOERT.name)
        return erFerdigstiltEllerJournalfoert
    }

    private suspend fun hentJournalpostInfoFraSaf(oppdatertJournalpost: PunsjJournalpost) =
        journalpostService.hentJournalpostInfo(oppdatertJournalpost.journalpostId)

    private suspend fun hentOgOppdaterJournalpostFraDB(mottattJournalpost: JournalpostMottaksHaandteringDto) =
        journalpostService.hent(mottattJournalpost.journalpostId).copy(
            ytelse = mottattJournalpost.fagsakYtelseTypeKode,
            aktørId = pdlService.aktørIdFor(mottattJournalpost.brukerIdent)
        )
}
