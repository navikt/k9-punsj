package no.nav.k9punsj.journalpost.postmottak

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.ReserverSaksnummerDto
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.JournalpostInfo
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
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
        val brukerIdent = mottattJournalpost.brukerIdent
        val brukerAktørId =
            pdlService.aktørIdFor(brukerIdent) ?: throw IllegalStateException("Fant ikke aktørId for brukerIdent")

        val barnIdent = mottattJournalpost.barnIdent
        val pleietrengendeAktørId = barnIdent.takeUnless { it.isNullOrBlank() }
            ?.let { personService.finnAktørId(it) }

        val fagsakYtelseType = FagsakYtelseType.fraKode(mottattJournalpost.fagsakYtelseTypeKode)

        logger.info("Verifiserer at det ikke finnes eksisterende fagsak for pleietrengende når man reserverer saksnummer.")
        val eksisterendeSaksnummer = mottattJournalpost.saksnummer

        k9SakService.hentFagsaker(brukerIdent).first?.let { fagsaker ->
            fagsaker.firstOrNull { it.pleietrengendeAktorId == pleietrengendeAktørId }
                ?.takeIf { eksisterendeSaksnummer == null }?.let { eksisterendeFagsak ->
                    throw EksisterendeFagsakPåPleietrengendeException(
                        mottattJournalpost.journalpostId,
                        eksisterendeFagsak
                    )
                }
        }

        val oppdatertJournalpost = hentOgOppdaterJournalpostFraDB(mottattJournalpost)
        val safJournalpostinfo = hentJournalpostInfoFraSaf(oppdatertJournalpost)

        val saksnummer = if (eksisterendeSaksnummer.isNullOrBlank()) {
            logger.info("Reserverer saksnummer fra k9-sak for journalpost: ${mottattJournalpost.journalpostId}")
            val reservertSaksnummerDto = k9SakService.reserverSaksnummer(
                ReserverSaksnummerDto(
                    brukerAktørId = brukerAktørId,
                    pleietrengendeAktørId = pleietrengendeAktørId,
                    ytelseType = fagsakYtelseType
                )
            )

            reservertSaksnummerDto.saksnummer
        } else {
            logger.info("Bruker eksisterende saksnummer: $eksisterendeSaksnummer")
            eksisterendeSaksnummer
        }

        if (!erFerdigstiltEllerJournalført(safJournalpostinfo)) {
            val (_, ferdigstillingFeil) = oppdaterOgFerdigstillJournalpostMedSaksnummer(
                mottattJournalpost,
                oppdatertJournalpost,
                saksnummer
            )
            if (ferdigstillingFeil != null) {
                return null to ferdigstillingFeil
            }
            lagreTilDB(oppdatertJournalpost)
            opprettAksjonspunktOgSendTilK9Los(oppdatertJournalpost, mottattJournalpost)
        } else {
            logger.info("Journalpost er allerede ferdigstilt eller journalført")
        }

        return saksnummer to null
    }

    private suspend fun opprettAksjonspunktOgSendTilK9Los(
        oppdatertJournalpost: PunsjJournalpost,
        mottattJournalpost: JournalpostMottaksHaandteringDto,
    ) {
        val fagsakYtelseTypeKode = mottattJournalpost.fagsakYtelseTypeKode
        val type = oppdatertJournalpost.type
        val aksjonspunkt = Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET)
        logger.info("Oppretter aksjonspunkt og sender til K9Los for journalpost: ${oppdatertJournalpost.journalpostId}. Type: $type, ytelse: $fagsakYtelseTypeKode, aksjonspunkt: $aksjonspunkt")
        aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
            punsjJournalpost = oppdatertJournalpost,
            aksjonspunkt = aksjonspunkt,
            type = type,
            ytelse = fagsakYtelseTypeKode
        )
    }

    private suspend fun oppdaterOgFerdigstillJournalpostMedSaksnummer(
        mottattJournalpost: JournalpostMottaksHaandteringDto,
        oppdatertJournalpost: PunsjJournalpost,
        saksnummer: String,
    ): Pair<HttpStatusCode?, String?> {
        logger.info("Ferdigstiller journalpost: ${oppdatertJournalpost.journalpostId} med saksnummer: $saksnummer")
        val (httpStatusCode, _) = journalpostService.oppdaterOgFerdigstillForMottak(mottattJournalpost, saksnummer)

        return if (!httpStatusCode.is2xxSuccessful) {
            httpStatusCode to "Feil ved ferdigstilling av journalpost: ${oppdatertJournalpost.journalpostId}. HttpStatusCode: $httpStatusCode"
        } else {
            logger.info("Ferdigstilt journalpost: ${oppdatertJournalpost.journalpostId}")
            null to null
        }
    }

    private suspend fun lagreTilDB(oppdatertJournalpost: PunsjJournalpost) {
        logger.info("Lagrer journalpost til DB: ${oppdatertJournalpost.journalpostId}")
        journalpostService.lagre(punsjJournalpost = oppdatertJournalpost)
    }

    private fun erFerdigstiltEllerJournalført(safJournalpostinfo: JournalpostInfo?): Boolean {
        val safJournalpostStatus = safJournalpostinfo?.journalpostStatus
        logger.info("safJournalpostStatus=$safJournalpostStatus")
        val erFerdigstiltEllerJournalfoert = (safJournalpostStatus == SafDtos.Journalstatus.FERDIGSTILT.name ||
                safJournalpostStatus == SafDtos.Journalstatus.JOURNALFOERT.name)

        logger.info("Sjekker om journalpost erFerdigstiltEllerJournalført= $erFerdigstiltEllerJournalfoert")
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
