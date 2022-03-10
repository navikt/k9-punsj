package no.nav.k9punsj.notat

import no.nav.k9punsj.azuregraph.IAzureGraphService
import no.nav.k9punsj.journalpost.*
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.util.*

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
            eksternReferanseId = UUID.randomUUID().toString(),
            tittel = notat.tittel,
            brevkode = "K9_PUNSJ_INNSENDING",
            tema = "OMS",
            kanal = Kanal.INGEN_DISTRIBUSJON,
            journalposttype = JournalpostType.NOTAT,
            dokumentKategori = DokumentKategori.IS,
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
