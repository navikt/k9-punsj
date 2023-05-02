package no.nav.k9punsj.innsending.journalforjson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.kodeverk.Fagsystem
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal object JournalførJsonMelding {

    internal fun hentBehov(packet: JsonMessage, aktueltBehov: String) = JournalførJson(
        json = packet[aktueltBehov.json()] as ObjectNode,
        tittel = packet[aktueltBehov.tittel()].hentString(),
        farge = packet[aktueltBehov.farge()].farge(),
        fagsystem = Fagsystem.K9SAK.kode,
        identitetsnummer = packet[aktueltBehov.identitetsnummer()].hentString().somIdentitetsnummer(),
        saksnummer = packet[aktueltBehov.saksnummer()].hentString(),
        brevkode = packet[aktueltBehov.brevkode()].hentString(),
        mottatt = packet[aktueltBehov.mottatt()].let { ZonedDateTime.parse(it.hentString()) },
        avsenderNavn = packet[aktueltBehov.avsenderNavn()].hentString()
    )

    private fun JsonNode.farge() = when {
        isMissingOrNull() -> DEFAULT_FARGE
        hentString().matches(FARGE_REGEX) -> hentString()
        else -> DEFAULT_FARGE.also {
            logger.warn("Ugyldig farge=[${hentString()} satt i meldingen, defaulter til farge=[$it]")
        }
    }

    internal data class JournalførJson(
        internal val json: ObjectNode,
        internal val brevkode: String,
        internal val tittel: String,
        internal val mottatt: ZonedDateTime,
        internal val farge: String,
        internal val fagsystem: String,
        internal val identitetsnummer: Identitetsnummer,
        internal val saksnummer: String,
        internal val avsenderNavn: String
    )

    private val logger = LoggerFactory.getLogger(JournalførJsonMelding::class.java)
    private val FARGE_REGEX = "#[a-fA-F0-9]{6}".toRegex()
    private const val DEFAULT_FARGE = "#C1B5D0"
    private fun String.json() = "@behov.$this.json"
    private fun String.brevkode() = "@behov.$this.brevkode"
    private fun String.mottatt() = "@behov.$this.mottatt"
    private fun String.tittel() = "@behov.$this.tittel"
    private fun String.farge() = "@behov.$this.farge"
    private fun String.fagsystem() = "@behov.$this.fagsystem"
    private fun String.identitetsnummer() = "@behov.$this.identitetsnummer"
    private fun String.saksnummer() = "@behov.$this.saksnummer"
    private fun String.avsenderNavn() = "@behov.$this.avsender.navn"
    private fun JsonNode.hentString() = asText().replace("\"","")
}