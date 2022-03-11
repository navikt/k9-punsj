package no.nav.k9punsj.notat

import no.nav.k9punsj.azuregraph.IAzureGraphService
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.journalpost.*
import org.json.JSONObject
import org.springframework.stereotype.Service
import kotlin.coroutines.coroutineContext

@Service
class NotatService(
    private val journalpostService: JournalpostService,
    private val notatPDFGenerator: NotatPDFGenerator,
    private val azureGraphService: IAzureGraphService,
) {
    suspend fun opprettNotat(notat: NyNotat): JournalPostResponse {
        val innloggetBrukerIdent = azureGraphService.hentIdentTilInnloggetBruker()
        val innloggetBrukerEnhet = azureGraphService.hentEnhetForInnloggetBruker()

        val notatPdf =
            notatPDFGenerator.genererPDF(notat.mapTilNotatOpplysninger(innloggetBrukerIdent, innloggetBrukerEnhet))

        val journalPostRequest = JournalPostRequest(
            eksternReferanseId = coroutineContext.hentCorrelationId(),
            tittel = notat.tittel,
            brevkode = "K9_PUNSJ_INNSENDING",
            tema = Tema.OMS,
            kanal = Kanal.INGEN_DISTRIBUSJON,
            journalposttype = JournalpostType.NOTAT,
            dokumentkategori = DokumentKategori.IS,
            fagsystem = FagsakSystem.K9,
            sakstype = SaksType.FAGSAK,
            saksnummer = notat.fagsakId,
            brukerIdent = notat.søkerIdentitetsnummer,
            avsenderNavn = innloggetBrukerIdent,
            tilleggsopplysninger = listOf(),
            pdf = notatPdf,
            json = JSONObject(notat)
        )

        return journalpostService.opprettJournalpost(journalPostRequest)
    }

    private fun NyNotat.mapTilNotatOpplysninger(innloggetBrukerIdentitetsnumer: String, innloggetBrukerEnhet: String) =
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
