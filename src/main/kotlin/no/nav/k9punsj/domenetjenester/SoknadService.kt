package no.nav.k9punsj.domenetjenester

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.kodeverk.Fagsystem
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.felles.Søknadstype
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.hentCorrelationId
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
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.integrasjoner.sak.SakClient
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.metrikker.SøknadMetrikkService
import no.nav.k9punsj.utils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.coroutines.coroutineContext

@Service
class SoknadService(
    private val journalpostService: JournalpostService,
    private val søknadRepository: SøknadRepository,
    private val søknadMetrikkService: SøknadMetrikkService,
    private val safGateway: SafGateway,
    private val k9SakService: K9SakService,
    private val sakClient: SakClient,
    private val pdlService: PdlService,
    private val dokarkivGateway: DokarkivGateway,
    private val bunkeRepository: BunkeRepository
) {

    internal suspend fun sendSøknad(
        søknad: Søknad,
        brevkode: Brevkode,
        journalpostIder: MutableSet<String>
    ): Pair<HttpStatus, String>? {
        val correlationId = try { coroutineContext.hentCorrelationId() } catch (e: Exception) { UUID.randomUUID().toString() }

        val journalpostIdListe = journalpostIder.toList()
        val journalposterKanSendesInn = journalpostService.kanSendeInn(journalpostIdListe)
        val punsjetAvSaksbehandler = hentSistEndretAvSaksbehandler(søknad.søknadId.id)

        val søkerFnr = søknad.søker.personIdent.verdi
        val k9YtelseType = Søknadstype.fraBrevkode(brevkode).k9YtelseType
        val fagsakYtelseType = no.nav.k9punsj.felles.FagsakYtelseType.fromKode(k9YtelseType)

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


        val fagsakIder = journalposter.filterNotNull()
            .filterNot { it.sak?.fagsakId.isNullOrEmpty() }
            .map { it.journalpostId to it.sak?.fagsakId }
            .toSet()

        /*
        * Bruker fagsakId fra journalposten om den finnes, ellers henter vi den fra k9sak
        * Kaster feil om vi har fler æn 1 unik fagsakId
        */
        val k9Saksnummer = if(fagsakIder.isNotEmpty()) {
            if(fagsakIder.size > 1) {
                throw IllegalStateException("Fant flere fagsakIder på innsending: ${fagsakIder.map { it.second }}")
            }
            fagsakIder.map {
                logger.info("Journalpost ${it.first} knyttet til fagsakId ${it.second}")
            }
            fagsakIder.first().second
        } else {
            val k9Respons = k9SakService.hentEllerOpprettSaksnummer(søknad.søknadId.toString())
            require(k9Respons.second.isNullOrBlank()) { "Feil ved henting av saksnummer: ${k9Respons.second}" }
            logger.info("Fick saksnummer (${k9Respons.second} av K9Sak for Journalpost ${journalpostIder.first()}")
            k9Respons.first
        }

        require(k9Saksnummer != null) { "K9Saksnummer er null" }

        // Sikkrer att saken kommer opp som valg i modia, ikke vart implementert sedan flytten till synkron
        //sakClient.forsikreSakskoblingFinnes(k9Saksnummer, søknad.søker.toString(), UUID.randomUUID().toString())

        // Ferdigstill journalposter
        val søkerNavn = pdlService.hentPersonopplysninger(setOf(søkerFnr))
        require(søkerNavn.isNotEmpty()) { throw IllegalStateException("Fant ikke søker i PDL") }
        val bruker = FerdigstillJournalpost.Bruker(
            identitetsnummer = søkerFnr.somIdentitetsnummer(),
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

        // TODO: Håndtere om vi manglerAvsendernavn?
        val manglerAvsendernavn = ferdigstillJournalposter.filter { it.manglerAvsendernavn() }
        require(manglerAvsendernavn.isEmpty()) {
            "Mangler avsendernavn på journalposter=[${manglerAvsendernavn.map { it.journalpostId }}]"
        }

        // Alle journalposter klare til oppdatering & ferdigstilling
        check(ferdigstillJournalposter.all { it.kanFerdigstilles }).also {
            logger.info("Journalposter klare for ferdigstilling: ${ferdigstillJournalposter.map { it.journalpostId }}")
        }

        ferdigstillJournalposter.forEach { ferdigstillJournalpost ->
            dokarkivGateway.oppdaterJournalpostForFerdigstilling(ferdigstillJournalpost)
            dokarkivGateway.ferdigstillJournalpost(ferdigstillJournalpost.journalpostId.toString(), "9999")
            logger.info("Ferdigstilt journalpost=[${ferdigstillJournalpost.journalpostId}]")
        }

        val søknadObject = objectMapper().convertValue<ObjectNode>(søknad)
        søknadObject.put("punsjet av", punsjetAvSaksbehandler)

        // Journalfør o ferdigstill søknadjson
        val pdf = PdfGenerator.genererPdf(
            html = HtmlGenerator.genererHtml(
                tittel = "Innsending fra Punsj",
                json = søknadObject
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
            brukerIdent = søkerFnr,
            avsenderNavn = punsjetAvSaksbehandler,
            pdf = pdf,
            json = søknadObject
        )

        val journalpostId = dokarkivGateway.opprettOgFerdigstillJournalpost(nyJournalpostRequest).journalpostId.somJournalpostId()
        logger.info("Opprettet Oppsummerings-PDF for PunsjetSøknad. JournalpostId=[$journalpostId]")

        // Send in søknad til k9sak
        k9SakService.sendInnSoeknad(
            soknad = søknad,
            journalpostId = journalpostId.toString(),
            fagsakYtelseType = fagsakYtelseType,
            saksnummer = k9Saksnummer,
            brevkode = brevkode
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

    suspend fun henteYtelsetypeForSøknad(søknadId: String): FagsakYtelseType? {
        return hentSøknad(søknadId)?.bunkeId?.let { bunkeId ->
            bunkeRepository.hentYtelseTypeForBunke(bunkeId)
        }
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
