package no.nav.k9punsj.journalpost.postmottak

import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.k9sak.Fagsak
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
    private val personService: PersonService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(PostMottakService::class.java)
    }

    suspend fun klassifiserOgJournalfør(mottattJournalpost: JournalpostMottaksHaandteringDto): Pair<Any?, String?> {
        val barnIdent = mottattJournalpost.barnIdent
        val pleietrengendeAktørId = barnIdent.takeUnless { it.isNullOrBlank() }
            ?.let { personService.finnPersonVedNorskIdent(it)?.aktørId }

        logger.info("Verifiserer at det ikke det ikke finnes eksisterende fagsak for pleietrengende når man reserverer saksnummer.")
        k9SakService.hentFagsaker(mottattJournalpost.brukerIdent).first?.let { fagsaker ->
            fagsaker.firstOrNull { it.pleietrengendeAktorId == pleietrengendeAktørId }
                ?.takeIf { mottattJournalpost.saksnummer == null }?.let {
                    return null to "Kunne ikke reservere saksnummer. Fagsak (${it.saksnummer}) finnes allerede for pleietrengende."
                }
        }

        val oppdatertJournalpost = hentOgOppdaterJournalpostFraDB(mottattJournalpost)
        val safJournalpostinfo = hentJournalpostInfoFraSaf(oppdatertJournalpost)

        val (saksnummer, feil) = mottattJournalpost.saksnummer.takeUnless { it.isNullOrBlank() }?.let { it: String ->
            logger.info("Bruker eksisterende saksnummer: $it")
            it to null
        } ?: run {
            logger.info("Reserverer saksnummer fra k9-sak for journalpost: ${mottattJournalpost.journalpostId}")
            return when {
                barnIdent.isNullOrBlank() -> k9SakService.reserverSaksnummer()
                else -> k9SakService.reserverSaksnummer(barnIdent)

            }.also { (reservertSaksnummerDto, feil) ->
                if (feil != null) {
                    return null to feil
                }
                if (reservertSaksnummerDto == null) {
                    logger.error("Saksnummer er null")
                    return null to "Saksnummer er null"
                } else {
                    logger.info("Bruker reservert saksnummer: ${reservertSaksnummerDto.saksnummer}")
                    reservertSaksnummerDto.saksnummer to null
                }
            }
        }

        if (!erFerdigstiltEllerJournalført(safJournalpostinfo)) {
            oppdaterOgFerdigstillJournalpostMedSaksnummer(mottattJournalpost, oppdatertJournalpost, saksnummer)
            lagreTilDB(oppdatertJournalpost)
            opprettAksjonspunktOgSendTilK9Los(oppdatertJournalpost, mottattJournalpost)
        }

        return saksnummer to null
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
        saksnummer: String,
    ) {
        logger.info("Ferdigstiller journalpost: ${oppdatertJournalpost.journalpostId} med saksnummer: $saksnummer")
        journalpostService.oppdaterOgFerdigstillForMottak(mottattJournalpost, saksnummer)
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

    private suspend fun hentJournalpostInfoFraSaf(oppdatertJournalpost: PunsjJournalpost): JournalpostInfo? {
        logger.info("Henter journalpostinfo fra SAF: ${oppdatertJournalpost.journalpostId}")
        return journalpostService.hentJournalpostInfo(oppdatertJournalpost.journalpostId)
    }

    private suspend fun hentOgOppdaterJournalpostFraDB(mottattJournalpost: JournalpostMottaksHaandteringDto): PunsjJournalpost {
        logger.info("Henter og oppdaterer journalpost fra DB: ${mottattJournalpost.journalpostId}")
        val aktørId = pdlService.aktørIdFor(mottattJournalpost.brukerIdent)
        return journalpostService.hent(mottattJournalpost.journalpostId).copy(
            ytelse = mottattJournalpost.fagsakYtelseTypeKode,
            aktørId = aktørId
        )
    }
}
