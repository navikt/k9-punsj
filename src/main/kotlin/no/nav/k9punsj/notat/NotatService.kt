package no.nav.k9punsj.notat

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.readValues
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
import no.nav.k9punsj.tilgangskontroll.azuregraph.IAzureGraphService
import no.nav.k9punsj.utils.objectMapper
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
            notatPDFGenerator.genererPDF(
                notat.mapTilNotatOpplysninger(
                    innloggetBrukerIdentitetsnumer = innloggetBrukerIdent,
                    innloggetBrukerEnhet = innloggetBrukerEnhet,
                    søkerNavn = person.navn()
                )
            )

        val notatJson = objectMapper().writeValueAsString(notat)
        val notatObject = objectMapper().readValue<ObjectNode>(notatJson)

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
            saksnummer = notat.fagsakId,
            brukerIdent = notat.søkerIdentitetsnummer,
            avsenderNavn = innloggetBrukerIdent,
            pdf = notatPdf,
            json = notatObject
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
