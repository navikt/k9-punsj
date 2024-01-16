package no.nav.k9punsj.journalpost.postmottak

import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.felles.dto.SaksnummerDto
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
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
    private val k9SakService: K9SakService,
    private val aksjonspunktService: AksjonspunktService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(PostMottakService::class.java)
    }

    suspend fun klassifiserOgJournalfør(mottattJournalpost: JournalpostMottaksHaandteringDto): Pair<SaksnummerDto?, String?> {
        val oppdatertJournalpost = hentOgOppdaterJournalpostFraDB(mottattJournalpost)
        val safJournalpostinfo = hentJournalpostInfoFraSaf(oppdatertJournalpost)

        val (saksnummerDto, feil) = k9SakService.reserverSaksnummer()
        if (feil != null) {
            return Pair(null, feil)
        }
        if(saksnummerDto == null) {
            return Pair(null, "Saksnummer er null")
        }

        if (!erFerdigstiltEllerJournalført(safJournalpostinfo)) {
            oppdaterOgFerdigstillJournalpostMedSaksnummer(mottattJournalpost, oppdatertJournalpost, saksnummerDto)
            lagreTilDB(oppdatertJournalpost)
            opprettAksjonspunktOgSendTilK9Los(oppdatertJournalpost, mottattJournalpost)
        }

        return Pair(saksnummerDto, null)
    }

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
        saksnummerDto: SaksnummerDto
    ) {
        journalpostService.oppdaterOgFerdigstillForMottak(mottattJournalpost, saksnummerDto)
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
