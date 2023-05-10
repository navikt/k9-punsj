package no.nav.k9punsj.innsending

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import no.nav.k9.kodeverk.Fagsystem
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.innsending.dto.NyJournalpost
import no.nav.k9punsj.innsending.dto.somPunsjetSøknad
import no.nav.k9punsj.innsending.journalforjson.HtmlGenerator
import no.nav.k9punsj.innsending.journalforjson.PdfGenerator
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.FerdigstillJournalpost
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.k9sak.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.SendPunsjetSoeknadTilK9SakGrunnlag
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.utils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@StandardProfil
@Qualifier("Rest")
@ConditionalOnProperty("innsending.rest.enabled", havingValue = "true", matchIfMissing = false)
class RestInnsendingClient(
    private val k9SakService: K9SakService,
    private val safGateway: SafGateway,
    private val pdlService: PdlService,
    private val dokarkivGateway: DokarkivGateway
) : InnsendingClient {

    override fun send(pair: Pair<String, String>) {
        // Mappe om json til object
        val json = objectMapper().readTree(pair.second)
        val correlationId = json["@correlationId"].asText()
        val punsjetSøknadJson = json["@behov"]["PunsjetSøknad"]
        val søknadJson = punsjetSøknadJson["søknad"] as ObjectNode
        val fagsakYtelseType = FagsakYtelseType.fraNavn(søknadJson["ytelse"]["type"].asText())

        val søknadsType = punsjetSøknadJson["søknadtype"].asText()
        val brevkode = Brevkode.fraKode(søknadsType)

        // TODO: Settes denne før innsending ?
        var k9Saksnummer = punsjetSøknadJson["saksnummer"]?.asText()

        val søknad = søknadJson.somPunsjetSøknad(
            versjon = punsjetSøknadJson["versjon"].asText(),
            saksbehandler = punsjetSøknadJson["saksbehandler"].saksbehandler(),
            brevkode = brevkode,
            saksnummer = k9Saksnummer
        )

        // Hent k9saksnummer
        if(k9Saksnummer.isNullOrEmpty()) {
            k9Saksnummer = runBlocking {
                val k9SaksnummerGrunnlag = HentK9SaksnummerGrunnlag(
                    søknadstype = fagsakYtelseType,
                    søker = søknad.søker.toString(),
                    pleietrengende = søknad.pleietrengende.toString(),
                    annenPart = søknad.annenPart.toString(),
                    journalpostId = søknad.journalpostIder.first().toString()
                )
                k9SakService.hentEllerOpprettSaksnummer(k9SaksnummerGrunnlag)
            }.first
        }

        requireNotNull(k9Saksnummer) { "K9Saksnummer er null" }

        // Ferdigstill journalposter
        val bruker = runBlocking {
            val søkerNavn = pdlService.hentPersonopplysninger(setOf(søknad.søker.toString()))
            require(søkerNavn.isNotEmpty()) { throw IllegalStateException("Fant ikke søker i PDL") }
            FerdigstillJournalpost.Bruker(
                identitetsnummer = søknad.søker,
                navn = søkerNavn.first().navn(),
                sak = Fagsystem.K9SAK to Saksnummer(k9Saksnummer)
            )
        }

        val ferdigstillJournalposter = søknad.journalpostIder.map { journalpostId ->
            runBlocking {
                safGateway.hentFerdigstillJournalpost(
                    correlationId = correlationId,
                    journalpostId = journalpostId
                )
            }
        }.filterNot { ferdigstillJournalpost ->
            ferdigstillJournalpost.erFerdigstilt.also {
                if (it) {
                    logger.info("JournalpostId=[${ferdigstillJournalpost.journalpostId}] er allerede ferdigstilt.")
                }
            }
        }.map { it.copy(bruker = bruker) }

        val manglerAvsendernavn = ferdigstillJournalposter.filter { it.manglerAvsendernavn() }

        require(manglerAvsendernavn.isEmpty()) {
            "Mangler avsendernavn på journalposter=[${manglerAvsendernavn.map { it.journalpostId }}]"
        }

        // Alle journalposter klare til oppdatering & ferdigstilling
        check(ferdigstillJournalposter.all { it.kanFerdigstilles })

        runBlocking {
            ferdigstillJournalposter.forEach { ferdigstillJournalpost ->
                dokarkivGateway.oppdaterJournalpostForFerdigstilling(correlationId, ferdigstillJournalpost)
                dokarkivGateway.ferdigstillJournalpost(ferdigstillJournalpost.journalpostId.toString(), "9999")
            }
        }

        // Journalfør o ferdigstill søknadjson
        val pdf = PdfGenerator.genererPdf(
            html = HtmlGenerator.genererHtml(
                tittel = "PunsjetSøknad",
                farge = søknadJson.farge(),
                json = søknadJson
            )
        )

        val journalpostId = runBlocking {
            val nyJournalpost = NyJournalpost(
                correlationId = correlationId,
                tittel = "PunsjetSøknad",
                brevkode = brevkode.kode,
                fagsystem = Fagsystem.K9SAK.kode,
                saksnummer = k9Saksnummer,
                identitetsnummer = bruker.identitetsnummer,
                avsenderNavn = bruker.navn!!,
                pdf = pdf,
                json = søknadJson
            )
            dokarkivGateway.opprettJournalpost(correlationId, nyJournalpost)
        }
        logger.info("Opprettet JournalpostId=[$journalpostId]")

        // Send in søknad til k9sak
        val søknadGrunnlag = SendPunsjetSoeknadTilK9SakGrunnlag(
            saksnummer = k9Saksnummer,
            journalpostId = journalpostId,
            referanse = correlationId
        )

        runBlocking {
            k9SakService.sendInnSoeknad(
                soeknad = søknad,
                grunnlag = søknadGrunnlag
            )
        }
    }

    private fun JsonNode.saksbehandler() = when (isMissingOrNull()) {
        true -> "n/a"
        false -> asText()
    }

    private fun JsonNode.farge() = when {
        isMissingOrNull() -> DEFAULT_FARGE
        hentString().matches(FARGE_REGEX) -> hentString()
        else -> DEFAULT_FARGE.also {
            logger.warn("Ugyldig farge=[${hentString()} satt i meldingen, defaulter til farge=[$it]")
        }
    }

    private fun JsonNode.isMissingOrNull() = isMissingNode || isNull

    override fun toString(): String = "RestInnsendingClient"

    private companion object {
        val logger = LoggerFactory.getLogger(RestInnsendingClient::class.java)
        val FARGE_REGEX = "#[a-fA-F0-9]{6}".toRegex()
        const val DEFAULT_FARGE = "#C1B5D0"
        private fun JsonNode.hentString() = asText().replace("\"", "")
    }
}
