package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9.kodeverk.Fagsystem
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.ytelse.Ytelse
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.felles.dto.SaksnummerDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.innsending.InnsendingClient
import no.nav.k9punsj.innsending.dto.somPunsjetSøknad
import no.nav.k9punsj.innsending.journalforjson.HtmlGenerator
import no.nav.k9punsj.innsending.journalforjson.PdfGenerator
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.DokumentKategori
import no.nav.k9punsj.integrasjoner.dokarkiv.FagsakSystem
import no.nav.k9punsj.integrasjoner.dokarkiv.FerdigstillJournalpost
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalPostRequest
import no.nav.k9punsj.integrasjoner.dokarkiv.JournalpostType
import no.nav.k9punsj.integrasjoner.dokarkiv.Kanal
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SaksType
import no.nav.k9punsj.integrasjoner.dokarkiv.Tema
import no.nav.k9punsj.integrasjoner.k9sak.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.PunsjetSøknad
import no.nav.k9punsj.integrasjoner.k9sak.dto.SendPunsjetSoeknadTilK9SakGrunnlag
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.metrikker.SøknadMetrikkService
import no.nav.k9punsj.utils.objectMapper
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
internal class SoknadService(
    private val journalpostService: JournalpostService,
    private val søknadRepository: SøknadRepository,
    private val søknadMetrikkService: SøknadMetrikkService,
    private val safGateway: SafGateway,
    private val k9SakService: K9SakService,
    private val pdlService: PdlService,
    private val dokarkivGateway: DokarkivGateway
) {

    internal suspend fun sendSøknad(
        søknad: Søknad,
        brevkode: Brevkode,
        journalpostIder: MutableSet<String>
    ): Pair<HttpStatus, String>? {
        val journalpostIdListe = journalpostIder.toList()
        val journalposterKanSendesInn = journalpostService.kanSendeInn(journalpostIdListe)
        val punsjetAvSaksbehandler = søknadRepository.hentSøknad(søknad.søknadId.id)?.endret_av!!.replace("\"", "")

        if (!journalposterKanSendesInn) {
            return HttpStatus.CONFLICT to "En eller alle journalpostene $journalpostIder har blitt sendt inn fra før"
        }

        val journalposter = safGateway.hentJournalposter(journalpostIdListe)
        val journalposterMedTypeUtgaaende = journalposter.filterNotNull()
            .filter { it.journalposttype == SafDtos.JournalpostType.U.toString() }
            .map { it.journalpostId }
            .toSet()
        if (journalposterMedTypeUtgaaende.isNotEmpty()) {
            return HttpStatus.CONFLICT to "Journalposter av type utgående ikke støttet: $journalposterMedTypeUtgaaende"
        }

        val journalposterMedStatusFeilregistrert = journalposter.filterNotNull()
            .filter { it.journalstatus != null }
            .filter { it.journalstatus!! == SafDtos.Journalstatus.FEILREGISTRERT.toString() }
            .map { it.journalpostId }
            .toSet()
        if (journalposterMedStatusFeilregistrert.isNotEmpty()) {
            return HttpStatus.CONFLICT to "Journalposter med status feilregistrert ikke støttet: $journalposterMedStatusFeilregistrert"
        }
        /*
                try {
                    innsendingClient.sendSøknad(
                        søknadId = søknad.søknadId.id,
                        søknad = søknad,
                        correlationId = UUID.randomUUID().toString(), // TODO: Erstattes med f.eks. LogFilter
                        tilleggsOpplysninger = mapOf(
                            PunsjetAvSaksbehandler to punsjetAvSaksbehandler,
                            Søknadtype to brevkode.kode
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Feil vid innsending av søknad for journalpostIder: ${journalpostIder.joinToString(", ")}")
                    return Pair(HttpStatus.INTERNAL_SERVER_ERROR, e.stackTraceToString())
                }
        */
        val fagsakYtelseType = søknad.getYtelse<Ytelse>()
        val søknadJson = objectMapper().writeValueAsString(søknad)

        // Hent k9saksnummer
        val k9SaksnummerGrunnlag = HentK9SaksnummerGrunnlag(
            søknadstype = fagsakYtelseType,
            søker = søknad.søker.toString(),
            pleietrengende = søknad.berørtePersoner.pleietrengende.toString(),
            annenPart = søknad.annenPart.toString(),
            journalpostId = journalpostIder.first() // TODO: Brukes for å utlede dato? Vilken journalpost er riktig?
        )
        val k9Saksnummer = k9SakService.hentEllerOpprettSaksnummer(k9SaksnummerGrunnlag).first?.let {
            objectMapper().readValue<SaksnummerDto>(it).saksnummer
        }

        // Ferdigstill journalposter
        val søkerNavn = pdlService.hentPersonopplysninger(setOf(søknad.søker.toString()))
        require(søkerNavn.isNotEmpty()) { throw IllegalStateException("Fant ikke søker i PDL") }
        val bruker = FerdigstillJournalpost.Bruker(
            identitetsnummer = søknad.søker.personIdent.toString().somIdentitetsnummer(),
            navn = søkerNavn.first().navn(),
            sak = Fagsystem.K9SAK to Saksnummer(k9Saksnummer)
        )

        val ferdigstillJournalposter = journalpostIder.map { journalpostId ->
            safGateway.hentFerdigstillJournalpost(journalpostId = journalpostId.somJournalpostId())
        }.filterNot { ferdigstillJournalpost ->
            ferdigstillJournalpost.erFerdigstilt.also {
                if (it) {
                    logger.info("JournalpostId=[${ferdigstillJournalpost.journalpostId}] er allerede ferdigstilt.")
                }
            }
        }.map {
            it.copy(
                bruker = bruker,
                sak = FerdigstillJournalpost.Sak(
                    sakstype = "FAGSAK",
                    fagsaksystem = "K9",
                    fagsakId = k9Saksnummer
                )
            )
        }

        val manglerAvsendernavn = ferdigstillJournalposter.filter { it.manglerAvsendernavn() }

        require(manglerAvsendernavn.isEmpty()) {
            "Mangler avsendernavn på journalposter=[${manglerAvsendernavn.map { it.journalpostId }}]"
        }

        // Alle journalposter klare til oppdatering & ferdigstilling
        check(ferdigstillJournalposter.all { it.kanFerdigstilles }).also {
            logger.info("Journalposter klare for ferdigstilling: ${ferdigstillJournalposter.map { it.journalpostId }}")
        }

        ferdigstillJournalposter.forEach { ferdigstillJournalpost ->
            dokarkivGateway.oppdaterJournalpostForFerdigstilling(correlationId, ferdigstillJournalpost)
            dokarkivGateway.ferdigstillJournalpost(ferdigstillJournalpost.journalpostId.toString(), "9999")
            logger.info("Ferdigstilt journalpost=[${ferdigstillJournalpost.journalpostId}]")
        }

        // Journalfør o ferdigstill søknadjson
        val pdf = PdfGenerator.genererPdf(
            html = HtmlGenerator.genererHtml(
                tittel = "Innsending fra Punsj",
                json = søknad
            )
        )

        val nyJournalpostRequest = JournalPostRequest(
            eksternReferanseId = correlationId,
            tittel = "PunsjetSøknad",
            brevkode = K9_PUNSJ_INNSENDING_BREVKODE,
            tema = Tema.OMS,
            kanal = Kanal.INGEN_DISTRIBUSJON,
            journalposttype = JournalpostType.NOTAT,
            dokumentkategori = DokumentKategori.IS,
            fagsystem = FagsakSystem.K9,
            sakstype = SaksType.FAGSAK,
            saksnummer = k9Saksnummer!!,
            brukerIdent = søknad.søker.toString(),
            avsenderNavn = punsjetAvSaksbehandler,
            pdf = pdf,
            json = JSONObject(søknadJson)
        )

        val journalpostId = dokarkivGateway.opprettJournalpost(nyJournalpostRequest).journalpostId.somJournalpostId()
        logger.info("Opprettet Oppsummerings-PDF for PunsjetSøknad. JournalpostId=[$journalpostId]")

        // Send in søknad til k9sak
        val søknadGrunnlag = SendPunsjetSoeknadTilK9SakGrunnlag(
            saksnummer = k9Saksnummer!!,
            journalpostId = journalpostId,
            referanse = correlationId
        )

        k9SakService.sendInnSoeknad(
            soeknad = søknad,
            grunnlag = søknadGrunnlag
        )


        leggerVedPayload(søknad, journalpostIder)
        journalpostService.settAlleTilFerdigBehandlet(journalpostIdListe)
        logger.info("Punsj har market disse journalpostIdene $journalpostIder som ferdigbehandlet")
        søknadRepository.markerSomSendtInn(søknad.søknadId.id)

        søknadMetrikkService.publiserMetrikker(søknad)
        return null
    }

    suspend fun hentSøknad(søknadId: String): SøknadEntitet? {
        return søknadRepository.hentSøknad(søknadId)
    }

    suspend fun opprettSøknad(søknad: SøknadEntitet): SøknadEntitet {
        return søknadRepository.opprettSøknad(søknad)
    }

    suspend fun oppdaterSøknad(søknad: SøknadEntitet) {
        søknadRepository.oppdaterSøknad(søknad)
    }

    suspend fun hentAlleSøknaderForBunke(bunkerId: String): List<SøknadEntitet> {
        return søknadRepository.hentAlleSøknaderForBunke(bunkerId)
    }

    suspend fun hentSistEndretAvSaksbehandler(søknadId: String): String {
        return søknadRepository.hentSøknad(søknadId)?.endret_av!!.replace("\"", "")
    }

    private suspend fun leggerVedPayload(
        søknad: Søknad,
        journalpostIder: MutableSet<String>
    ) {
        val writeValueAsString = objectMapper().writeValueAsString(søknad)

        journalpostIder.forEach {
            val journalpost = journalpostService.hent(it)
            val medPayload = journalpost.copy(payload = writeValueAsString)
            journalpostService.lagre(medPayload)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SoknadService::class.java)
        const val K9_PUNSJ_INNSENDING_BREVKODE = "K9_PUNSJ_INNSENDING"
    }
}
