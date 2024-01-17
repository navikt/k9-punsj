package no.nav.k9punsj.integrasjoner.dokarkiv

import no.nav.k9.kodeverk.Fagsystem
import no.nav.k9.sak.typer.Saksnummer
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.JournalpostId
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal data class FerdigstillJournalpost(
    internal val journalpostId: JournalpostId,
    private val status: JoarkTyper.JournalpostStatus,
    private val type: JoarkTyper.JournalpostType,
    private val avsenderIdType: String? = null,
    private val tittel: String? = null,
    private val dokumenter: Set<Dokument> = emptySet(),
    private val bruker: Bruker? = null,
    private val sak: Sak,
) {

    private val mangler = mutableListOf<Mangler>().also { alleMangler ->
        if (bruker == null) {
            alleMangler.add(Mangler.Bruker)
        }
        if (avsenderIdType.isNullOrBlank() && bruker?.identitetsnummer == null && !type.erNotat) {
            alleMangler.add(Mangler.AvsenderIdType)
        }
    }.toList()

    internal val erFerdigstilt = status.erFerdigstilt || status.erJournalført
    internal val kanFerdigstilles = !erFerdigstilt

    internal fun oppdaterPayloadMedSak(): String {
        check(kanFerdigstilles) { "Journalposten $journalpostId kan ikke ferdigstilles." }
        val utfyllendeInformasjon = mutableListOf<String>()

        @Language("JSON")
        val json = """
        {
          "tema": "OMS",
          "bruker": {
            "idType": "FNR",
            "id": "${bruker!!.identitetsnummer}"
          }
        }
        """.trimIndent().let { JSONObject(it) }

        json.put("sak", JSONObject().also {
            it.put("sakstype", sak.sakstype)
            it.put("fagsakId", sak.fagsakId)
            it.put("fagsaksystem", sak.fagsaksystem)
        })

        // Mangler tittel på journalposten
        if (tittel.isNullOrBlank()) {
            utfyllendeInformasjon.add("tittel=[$ManglerTittel]")
            json.put("tittel", ManglerTittel)
        } else {
            json.put("tittel", tittel)
        }
        // Mangler tittel på et eller fler dokumenter
        dokumenter.filter { it.tittel.isNullOrBlank() }.takeIf { it.isNotEmpty() }?.also { dokumenterUtenTittel ->
            val jsonDokumenter = JSONArray()
            dokumenterUtenTittel.forEach { dokumentUtenTittel ->
                jsonDokumenter.put(
                    JSONObject().also {
                        it.put("dokumentInfoId", dokumentUtenTittel.dokumentId)
                        it.put("tittel", ManglerTittel)
                    }
                )
                utfyllendeInformasjon.add("dokumenter[${dokumentUtenTittel.dokumentId}].tittel=[$ManglerTittel]")
            }
            json.put("dokumenter", jsonDokumenter)
        }

        // Oppdaterer avsenderId hvis avsenderIdType mangler
        if (avsenderIdType.isNullOrBlank() && !type.erNotat) {
            logger.info("AvsenderIdType manglet for journalpost $journalpostId. Oppdaterer med avsenderMottaker.id=[***] og avsenderMottaker.idType=FNR")
            json.put("avsenderMottaker", JSONObject().also {
                it.put("idType", "FNR")
                it.put("id", bruker.identitetsnummer.toString())
            })
            utfyllendeInformasjon.add("avsenderMottaker.idType=FNR")
            utfyllendeInformasjon.add("avsenderMottaker.id=[***]")
        }

        return json.toString().also {
            if (utfyllendeInformasjon.isNotEmpty()) {
                logger.info("Utfyllende informasjon ved oppdatering: ${utfyllendeInformasjon.joinToString(", ")}")
            }
        }
    }

    private fun mangler(): List<Mangler> {
        check(!erFerdigstilt) { "Journalpost $journalpostId er allerede ferdigstilt." }
        return mangler
    }

    internal fun manglerAvsendernavn() = mangler().contains(Mangler.Avsendernavn)

    internal fun kanFerdigstilles() {
        check(kanFerdigstilles) { "Journalposten $journalpostId kan ikke ferdigstilles." }
    }

    internal data class Bruker(
        internal val identitetsnummer: Identitetsnummer,
        internal val sak: Pair<Fagsystem, Saksnummer>? = null,
        internal val navn: String? = null,
    )

    internal data class Dokument(
        internal val dokumentId: String,
        internal val tittel: String?,
    )

    internal data class Sak(
        internal val sakstype: String?,
        internal val fagsaksystem: String?,
        internal val fagsakId: String?,
    )

    internal enum class Mangler {
        Bruker,
        Avsendernavn,
        AvsenderIdType,
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(FerdigstillJournalpost::class.java)
        private const val ManglerTittel = "Mangler tittel"
        private const val FerdigstillPayload = """{"journalfoerendeEnhet": "9999"}"""
    }
}
