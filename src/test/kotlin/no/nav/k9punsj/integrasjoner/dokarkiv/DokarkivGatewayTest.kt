package no.nav.k9punsj.integrasjoner.dokarkiv

import no.nav.k9.kodeverk.Fagsystem
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.integrasjoner.dokarkiv.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import no.nav.k9punsj.integrasjoner.dokarkiv.JoarkTyper.JournalpostType.Companion.somJournalpostType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DokarkivGatewayTest {

    @Test
    fun `Oppdaterer journalpost med utfyllende info for ferdigstilling`() {
        val saksnummer = Saksnummer("1DP1GG8")
        val journalpost = FerdigstillJournalpost(
            journalpostId="598146191".somJournalpostId(),
            status="MOTTATT".somJournalpostStatus(),
            type="I".somJournalpostType(),
            avsendernavn="Pessimistisk, Bildekort",
            tittel="Søknad om utbetaling av omsorgspenger for arbeidstaker",
            dokumenter=setOf(FerdigstillJournalpost.Dokument(dokumentId="624895411", tittel="Søknad om utbetaling av omsorgspenger for arbeidstaker")),
            bruker= FerdigstillJournalpost.Bruker(
                identitetsnummer = "15069205221".somIdentitetsnummer(),
                sak = Pair(Fagsystem.K9SAK, saksnummer),
                navn="Pessimistisk Bildekort"
            ),
        sak= FerdigstillJournalpost.Sak(sakstype = "FAGSAK", fagsaksystem = "K9", fagsakId = saksnummer.toString())
        )

        val oppdatertJournalpost = journalpost.oppdaterPayloadMedSak()

        assertNotEquals(journalpost, oppdatertJournalpost)
    }
}