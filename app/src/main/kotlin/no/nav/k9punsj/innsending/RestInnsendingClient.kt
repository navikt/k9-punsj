package no.nav.k9punsj.innsending

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9.kodeverk.Fagsystem
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.felles.Søknadstype
import no.nav.k9punsj.felles.dto.SaksnummerDto
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
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SaksType
import no.nav.k9punsj.integrasjoner.dokarkiv.Tema
import no.nav.k9punsj.integrasjoner.k9sak.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.SendPunsjetSoeknadTilK9SakGrunnlag
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.utils.objectMapper
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@StandardProfil
@ConditionalOnProperty("innsending.rest.enabled", havingValue = "true", matchIfMissing = false)
class RestInnsendingClient(
    private val k9SakService: K9SakService,
    private val safGateway: SafGateway,
    private val pdlService: PdlService,
    private val dokarkivGateway: DokarkivGateway,
    private val kafkaKopierjournalpostInnsendingClient: KafkaKopierjournalpostInnsendingClient
) : InnsendingClient {

    private companion object {
        val logger = LoggerFactory.getLogger(RestInnsendingClient::class.java)

        /**
         * Brevkode som brukes i k9-sak som unntakshåndtering. Leder til att oppsummerings-pdfen punsj genererer ikke
         * havner i listen av dokumenter som skall klassifiseres under sykdom.
         */
        const val K9_PUNSJ_INNSENDING_BREVKODE = "K9_PUNSJ_INNSENDING"
    }

    override suspend fun send(pair: Pair<String, String>) {
        val json = objectMapper().readTree(pair.second)
        /* Unntakshåndtering for å kopiere journalpost, 2 ulike typer av "rapids"-behov.
        *  KopierJournalpost skall ikke sendes in til K9Sak.
        *  Sendes på Kafka å plukkes opp av punsjbollen for kopiering.
        */
        if (json["@behov"]["KopierPunsjbarJournalpost"] != null) {
            kafkaKopierjournalpostInnsendingClient.send(pair)
            return
        }

        // Mappe om json til object
        val correlationId = json["@correlationId"].asText()
        val punsjetSøknadJson = json["@behov"]["PunsjetSøknad"]
        val søknadJson = punsjetSøknadJson["søknad"] as ObjectNode
        val fagsakYtelseTypeKode = Søknadstype.fraK9FormatYtelsetype(søknadJson["ytelse"]["type"].asText()).k9YtelseType
        val søknadsType = punsjetSøknadJson["søknadtype"].asText()
        var k9Saksnummer = punsjetSøknadJson["saksnummer"]?.asText() // TODO: Settes denne før innsending ?

        // Workaround for f.eks. OMP_UT som ikke er en gyldig ytelsetype i K9
        val fagsakYtelseType = FagsakYtelseType.fromKode(fagsakYtelseTypeKode)

        require(fagsakYtelseType != FagsakYtelseType.UDEFINERT) {
            throw IllegalStateException("FagsakYtelseType er udefinert")
        }

        val brevkode = Brevkode.fraKode(søknadsType)

        val søknad = søknadJson.somPunsjetSøknad(
            versjon = punsjetSøknadJson["versjon"].asText(),
            saksbehandler = punsjetSøknadJson["saksbehandler"].saksbehandler(),
            brevkode = brevkode,
            saksnummer = k9Saksnummer
        )

        // Hent k9saksnummer
        if (k9Saksnummer.isNullOrEmpty()) {
            val k9SaksnummerGrunnlag = HentK9SaksnummerGrunnlag(
                søknadstype = fagsakYtelseType,
                søker = søknad.søker.toString(),
                pleietrengende = søknad.pleietrengende.toString(),
                annenPart = søknad.annenPart.toString(),
                journalpostId = søknad.journalpostIder.first().toString()
            )
            k9SakService.hentEllerOpprettSaksnummer(k9SaksnummerGrunnlag).first?.let {
                k9Saksnummer = objectMapper().readValue<SaksnummerDto>(it).saksnummer
            }
        }

        requireNotNull(k9Saksnummer) { "K9Saksnummer er null" }

        // Ferdigstill journalposter
        val søkerNavn = pdlService.hentPersonopplysninger(setOf(søknad.søker.toString()))
        require(søkerNavn.isNotEmpty()) { throw IllegalStateException("Fant ikke søker i PDL") }
        val bruker = FerdigstillJournalpost.Bruker(
            identitetsnummer = søknad.søker,
            navn = søkerNavn.first().navn(),
            sak = Fagsystem.K9SAK to Saksnummer(k9Saksnummer)
        )

        val ferdigstillJournalposter = søknad.journalpostIder.map { journalpostId ->
            safGateway.hentFerdigstillJournalpost(journalpostId = journalpostId)
        }.filterNot { ferdigstillJournalpost ->
            ferdigstillJournalpost.erFerdigstilt.also {
                if (it) {
                    logger.info("JournalpostId=[${ferdigstillJournalpost.journalpostId}] er allerede ferdigstilt.")
                }
            }
        }.map { it.copy(
            bruker = bruker,
            sak = FerdigstillJournalpost.Sak(
                sakstype = "FAGSAK",
                fagsaksystem = "K9",
                fagsakId = k9Saksnummer
            )
        ) }

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
                tittel = "PunsjetSøknad",
                json = søknadJson
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
            avsenderNavn = søknad.saksbehandler,
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
    }

    private fun JsonNode.saksbehandler() = when (isMissingOrNull()) {
        true -> "n/a"
        false -> asText()
    }

    private fun JsonNode.isMissingOrNull() = isMissingNode || isNull
    override fun toString(): String = "RestInnsendingClient"

}
