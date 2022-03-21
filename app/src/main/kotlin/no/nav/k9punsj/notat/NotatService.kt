package no.nav.k9punsj.notat

import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.hentCorrelationId
import no.nav.k9punsj.journalpost.*
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.rest.eksternt.pdl.Personopplysninger
import org.json.JSONObject
import org.springframework.stereotype.Service
import kotlin.coroutines.coroutineContext

@Service
class NotatService(
    private val journalpostService: JournalpostService,
    private val notatPDFGenerator: NotatPDFGenerator,
    private val azureGraphService: IAzureGraphService,
    private val pdlService: PdlService
) {
    suspend fun opprettNotat(notat: NyNotat): JournalPostResponse {
        val innloggetBrukerIdent = azureGraphService.hentIdentTilInnloggetBruker()
        val innloggetBrukerEnhet = azureGraphService.hentEnhetForInnloggetBruker()
        val person = pdlService.hentPersonopplysninger(setOf(notat.søkerIdentitetsnummer)).first()

        val notatPdf =
            notatPDFGenerator.genererPDF(notat.mapTilNotatOpplysninger(
                innloggetBrukerIdentitetsnumer = innloggetBrukerIdent,
                innloggetBrukerEnhet = innloggetBrukerEnhet,
                søkerNavn = person.navn()
            ))

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
            pdf = notatPdf,
            json = JSONObject(notat)
        )

        return journalpostService.opprettJournalpost(journalPostRequest)
    }

    private fun NyNotat.mapTilNotatOpplysninger(
        innloggetBrukerIdentitetsnumer: String,
        innloggetBrukerEnhet: String,
        søkerNavn: String
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

private fun Personopplysninger.navn(): String = when(mellomnavn) {
    null -> "$fornavn $etternavn"
    else -> "$fornavn $mellomnavn $etternavn"
}
