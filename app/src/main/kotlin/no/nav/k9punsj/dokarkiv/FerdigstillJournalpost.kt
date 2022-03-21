package no.nav.k9punsj.dokarkiv

import no.nav.k9.kodeverk.Fagsystem
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9punsj.journalpost.Identitetsnummer
import no.nav.k9punsj.journalpost.JournalpostId
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal data class FerdigstillJournalpost(
    internal val journalpostId: JournalpostId,
    private val status: JoarkTyper.JournalpostStatus,
    private val type: JoarkTyper.JournalpostType,
    private val avsendernavn: String? = null,
    private val tittel: String? = null,
    private val dokumenter: Set<Dokument> = emptySet(),
    private val bruker: Bruker? = null) {

    private val erFerdigstilt = status.erFerdigstilt || status.erJournalført
    private val kanFerdigstilles = !erFerdigstilt

    internal fun oppdaterPayloadGenerellSak() : String {
        check(kanFerdigstilles) { "Journalposten $journalpostId kan ikke ferdigstilles." }
        val utfyllendeInformasjon = mutableListOf<String>()
        @Language("JSON")
        val json = """
        {
          "tema": "OMS",
          "bruker": {
            "idType": "FNR",
            "id": "${bruker!!.identitetsnummer}"
          },
          "sak": {
            "sakstype": "GENERELL_SAK"
          }
        }
        """.trimIndent().let { JSONObject(it) }

        // Mangler tittel på journalposten
        if (tittel.isNullOrBlank()) {
            utfyllendeInformasjon.add("tittel=[$ManglerTittel]")
            json.put("tittel", ManglerTittel)
        }
        // Mangler tittel på et eller fler dokumenter
        dokumenter.filter { it.tittel.isNullOrBlank() }.takeIf { it.isNotEmpty() }?.also { dokumenterUtenTittel ->
            val jsonDokumenter = JSONArray()
            dokumenterUtenTittel.forEach { dokumentUtenTittel ->
                jsonDokumenter.put(JSONObject().also {
                    it.put("dokumentInfoId", dokumentUtenTittel.dokumentId)
                    it.put("tittel", ManglerTittel)
                })
                utfyllendeInformasjon.add("dokumenter[${dokumentUtenTittel.dokumentId}].tittel=[$ManglerTittel]")
            }
            json.put("dokumenter", jsonDokumenter)
        }
        // Mangler navn på avsender
        if (avsendernavn.isNullOrBlank() && !type.erNotat) {
            json.put("avsenderMottaker", JSONObject().also { it.put("navn", bruker.navn!!) })
            utfyllendeInformasjon.add("avsenderMottaker.navn=[***]")
        }
        return json.toString().also { if (utfyllendeInformasjon.isNotEmpty()) {
            logger.info("Utfyllende informasjon ved oppdatering: ${utfyllendeInformasjon.joinToString(", ")}")
        }}
    }

    internal fun kanFerdigstilles() {
        check(kanFerdigstilles) { "Journalposten $journalpostId kan ikke ferdigstilles." }
    }

    internal data class JournalførendeEnhet(
        internal val journalfoerendeEnhet: String
    )

    internal data class Bruker(
        internal val identitetsnummer: Identitetsnummer,
        internal val sak: Pair<Fagsystem, Saksnummer>? = null,
        internal val navn: String? = null
    )

    internal data class Dokument(
        internal val dokumentId: String,
        internal val tittel: String?
    )

    private companion object {
        private val logger = LoggerFactory.getLogger(FerdigstillJournalpost::class.java)
        private const val ManglerTittel = "Mangler tittel"
    }
}
