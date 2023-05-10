package no.nav.k9punsj.innsending.journalforjson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.ObjectNode

import org.intellij.lang.annotations.Language
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal object HtmlGenerator {

    internal fun genererHtml(
        tittel: String,
        farge: String,
        json: ObjectNode
    ): String {

        @Language("HTML")
        val html = """
            <html lang="no">
                <head>
                    <title>$tittel</title>
                    <style>
                        @page {
                            size: A4 portrait;
                            margin: 0.3cm 0.3cm 0.3cm;
                            padding-bottom: 1cm;
                        }
                        * {
                            font-family: "Source Sans Pro";
                        }
                        .json_object { margin:10px; padding-left:10px; border-left:1px solid #ccc }
                        .json_key { font-weight: bold; }
                        #header {
                            font-size: 18px;
                            font-weight: bold;
                            display: block;
                            color: #000;
                            padding: .4cm 0.7cm .4cm 2.2cm;
                            background: $farge url($NAV_LOGO) no-repeat .7cm center;
                            background-size: 1.2cm;
                        }
                        #json {
                            padding-top: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div id="header">$tittel</div>
                    <div id="json">${json.toHtml()}</div>
                </body>
            </html>
        """.trimIndent()
        return html
    }

    private fun JsonNode.toHtml(): String {
        if (!inneholderData()) {
            return ""
        }

        var html = ""

        when (this) {
            is ObjectNode -> {
                var objectHtml = ""
                fields().asSequence().sortedBy { it.key }.forEach { (navn, jsonNode) ->
                    if (jsonNode.inneholderData()) {
                        objectHtml += """<div><span class="json_key">${navn.formatKey()}</span>: ${jsonNode.toHtml()}</div>"""
                    }
                }
                if (objectHtml.isNotBlank()) {
                    html += """<div class="json_object">$objectHtml</div>"""
                }
            }

            is ArrayNode -> forEachIndexed { _, arrayElement ->
                if (arrayElement.inneholderData()) {
                    html += when (arrayElement.isObject) {
                        true -> arrayElement.toHtml()
                        false -> """<div class="json_object"><div><span class="json_key"></span>${arrayElement.toHtml()}</div></div>"""
                    }
                }
            }

            else -> html += formatValue()
        }

        return html
    }

    private fun String.formatKey() =
        removePrefix("_").let { utenPrefix -> utenPrefix.formatPeriodeOrNull() ?: utenPrefix.revertCamelCase() }

    private fun String.revertCamelCase(): String {
        var reverted = ""
        var wasDigit = false
        forEachIndexed { index, char ->
            reverted += when {
                index == 0 -> char.uppercase()
                char.isUpperCase() -> " ${char.lowercase()}"
                char.isDigit() && !wasDigit -> " $char"
                else -> char
            }
            wasDigit = char.isDigit()
        }
        return reverted
    }

    private fun JsonNode.formatValue(): String = when (this) {
        is BooleanNode -> when (this.booleanValue()) {
            true -> "Ja"
            false -> "Nei"
        }

        else -> {
            val textValue = asText()
            textValue.formatTimeOrNull()
                ?: textValue.formatDateOrNull()
                ?: textValue.formatDurationOrNull()
                ?: textValue.formatPeriodeOrNull()
                ?: textValue.escapeHTML()
        }
    }

    private fun Number.formatDurationPart(entall: String, flertall: String = "${entall}er") = when (this.toLong()) {
        0L -> ""
        1L -> "$this $entall, "
        else -> "$this $flertall, "
    }

    private fun String.formatDurationOrNull() = parseOrNull {
        Duration.parse(this).let {
            when (it.toSeconds() == 0L) {
                true -> "0 sekunder"
                else -> "${it.toDaysPart().formatDurationPart("dag")}${
                    it.toHoursPart().formatDurationPart("time", "timer")
                }${it.toMinutesPart().formatDurationPart("minutt")}${
                    it.toSecondsPart().formatDurationPart("sekund")
                }".removeSuffix(", ")
            }
        }
    }

    private fun String.formatPeriodeOrNull() = parseOrNull {
        when (this.matches(PERIODE_REGEX)) {
            true -> {
                val split = this.split("/")
                val fom = LocalDate.parse(split[0])
                val tom = LocalDate.parse(split[1])
                when (fom == tom) {
                    true -> fom.format(DATE_FORMATTER)
                    false -> "${fom.format(DATE_FORMATTER)} - ${tom.format(DATE_FORMATTER)}"
                }
            }

            false -> null
        }
    }

    private fun String.formatDateOrNull() = parseOrNull { LocalDate.parse(this).format(DATE_FORMATTER) }

    private fun String.formatTimeOrNull() = parseOrNull { ZonedDateTime.parse(this).format(DATE_TIME_FORMATTER) }

    private fun parseOrNull(parse: () -> String?) = kotlin.runCatching { parse() }.fold(
        onSuccess = { it },
        onFailure = { null }
    )

    private fun JsonNode.inneholderData(): Boolean {
        if (isMissingNode || isNull) {
            return false
        } else if (isArray) {
            return size() > 0
        } else if (isObject) {
            fields().forEach { (_, jsonNode) ->
                // Rekursivt kall som returnerer så fort den ser at en node inneholder data
                if (jsonNode.inneholderData()) {
                    return true
                }
            }
            return false
        }
        // Alle andre ting anser vi som data
        else {
            return asText().isNotBlank()
        }
    }

    private fun String.escapeHTML(): String {
        val text = this@escapeHTML
        if (text.isEmpty()) return text

        return buildString(length) {
            for (idx in 0 until text.length) {
                val ch = text[idx]
                when (ch) {
                    '\'' -> append("&#x27;")
                    '\"' -> append("&quot;")
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    else -> append(ch)
                }
            }
        }
    }

    private val ZONE_ID = ZoneId.of("Europe/Oslo")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZONE_ID)
    private val PERIODE_REGEX = "\\d{4}-\\d{2}-\\d{2}/\\d{4}-\\d{2}-\\d{2}".toRegex()
    private const val NAV_LOGO =
        "data:image/png;base64,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"
}