package no.nav.k9punsj.notat

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktService
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.fordel.K9FordelType
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.integrasjoner.dokarkiv.DokumentKategori
import no.nav.k9punsj.integrasjoner.dokarkiv.FagsakSystem
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalPostRequest
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalPostResponse
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalpostType
import no.nav.k9punsj.integrasjoner.dokarkiv.Kanal
import no.nav.k9punsj.integrasjoner.dokarkiv.SaksType
import no.nav.k9punsj.integrasjoner.dokarkiv.Tema
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.sak.SakService
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.utils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.coroutineContext

@Service
class NotatService(
    private val journalpostService: JournalpostService,
    private val notatPDFGenerator: NotatPDFGenerator,
    private val azureGraphService: IAzureGraphService,
    private val pdlService: PdlService,
    private val aksjonspunktService: AksjonspunktService,
    private val sakService: SakService,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(NotatService::class.java)
    }

    @Transactional
    suspend fun opprettNotat(notat: NyNotat): JournalPostResponse {
        logger.info("Oppretter journalføringsnotat med fagsakId=${notat.fagsakId}.")
        val innloggetBrukerIdent = azureGraphService.hentIdentTilInnloggetBruker()
        val innloggetBrukerEnhet = azureGraphService.hentEnhetForInnloggetBruker()
        val person = pdlService.hentPersonopplysninger(setOf(notat.søkerIdentitetsnummer)).first()
        val søkerAktørId = pdlService.aktørIdFor(person.identitetsnummer)

        logger.info("Henter fagsak med id=${notat.fagsakId}.")
        val fagsak = sakService.hentSaker(notat.søkerIdentitetsnummer)
            .firstOrNull { it.fagsakId == notat.fagsakId }
            ?: throw IllegalStateException("Finner ikke fagsak ${notat.fagsakId} for søker ${notat.søkerIdentitetsnummer}")


        logger.info("Genererer PDF for notat med fagsak [${fagsak.sakstype} - ${notat.fagsakId}].")
        val notatPdf =
            notatPDFGenerator.genererPDF(
                notat.mapTilNotatOpplysninger(
                    innloggetBrukerIdentitetsnumer = innloggetBrukerIdent,
                    innloggetBrukerEnhet = innloggetBrukerEnhet,
                    søkerNavn = person.navn()
                )
            )

        val notatObject = objectMapper().convertValue(notat, ObjectNode::class.java)
        val journalPostRequest = JournalPostRequest(
            eksternReferanseId = coroutineContext.hentCorrelationId(),
            tittel = notat.tittel,
            brevkode = "K9_PUNSJ_NOTAT",
            tema = Tema.OMS,
            kanal = Kanal.INGEN_DISTRIBUSJON,
            journalposttype = JournalpostType.NOTAT,
            dokumentkategori = DokumentKategori.IS,
            fagsystem = FagsakSystem.K9,
            sakstype = SaksType.FAGSAK,
            saksnummer = fagsak.fagsakId,
            brukerIdent = notat.søkerIdentitetsnummer,
            avsenderNavn = innloggetBrukerIdent,
            pdf = notatPdf,
            json = notatObject
        )

        logger.info("Oppretter journalpost i dokarkiv for notat med fagsak [${fagsak.sakstype} - ${notat.fagsakId}].")
        val journalPostResponse = journalpostService.opprettJournalpost(journalPostRequest)
        val journalpostId = journalPostResponse.journalpostId
        val journalførtTidspunkt = LocalDateTime.now()


        logger.info("Lagrer journalpost med id=$journalpostId i Punsj for notat med fagsak [${fagsak.sakstype} - ${notat.fagsakId}].")
        val punsjJournalpost = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = journalpostId,
            aktørId = søkerAktørId,
            skalTilK9 = true,
            mottattDato = journalførtTidspunkt,
            journalførtTidspunkt = journalførtTidspunkt,
            ytelse = fagsak.sakstype
        )
        journalpostService.opprettJournalpost(punsjJournalpost)

        logger.info("Oppretter aksjonspunkt i LOS for notat med fagsak [${fagsak.sakstype} - ${notat.fagsakId}].")
        aksjonspunktService.opprettAksjonspunktOgSendTilK9Los(
            punsjJournalpost = punsjJournalpost,
            aksjonspunkt = Pair(AksjonspunktKode.PUNSJ, AksjonspunktStatus.OPPRETTET),
            type = K9FordelType.JOURNALPOSTNOTAT.kode,
            ytelse = fagsak.sakstype,
            pleietrengendeAktørId = fagsak.pleietrengendeIdent
        )

        return journalPostResponse
    }

    private fun NyNotat.mapTilNotatOpplysninger(
        innloggetBrukerIdentitetsnumer: String,
        innloggetBrukerEnhet: String,
        søkerNavn: String,
    ) =
        NotatOpplysninger(
            søkerIdentitetsnummer = søkerIdentitetsnummer,
            søkerNavn = søkerNavn,
            fagsakId = fagsakId,
            tittel = tittel,
            notat = notat,
            saksbehandlerEnhet = innloggetBrukerEnhet,
            saksbehandlerNavn = innloggetBrukerIdentitetsnumer
        )
}
