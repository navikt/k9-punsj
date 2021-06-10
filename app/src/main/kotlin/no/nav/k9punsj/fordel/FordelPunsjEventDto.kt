package no.nav.k9punsj.fordel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.AktørIdDto
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class FordelPunsjEventDto(
    val aktørId: AktørIdDto? = null,
    val journalpostId: JournalpostIdDto,
    val type: String? = null,
    val ytelse: String? = null,
    val opprinneligJournalpost: OpprinneligJournalpost? = null) {

    data class OpprinneligJournalpost(
        val journalpostId: JournalpostIdDto,
    )

    internal companion object {

        private val logger: Logger = LoggerFactory.getLogger(FordelPunsjEventDto::class.java)
        private val objectMapper = objectMapper()
        private val maskertText = TextNode("***")

        internal fun String.somFordelPunsjEventDto(topic: String) : FordelPunsjEventDto {
            val maskert = kotlin.runCatching { objectMapper.readTree(this).let { it as ObjectNode }.also {
                if (it.hasNonNull("aktørId")) {
                    it.replace("aktørId", maskertText)
                }
            }.toString()}.fold(onSuccess = {it}, onFailure = {
                throw IllegalStateException("Mottatt melding på Topic=[$topic] som ikke er gyldig JSON. Melding=$this", it)
            })

            return kotlin.runCatching { objectMapper.readValue<FordelPunsjEventDto>(this).also {
                logger.info("Mottatt punsjbar journalpost på Topic=[$topic], PunsjbarJournalpost=$maskert")
            }}.fold(onSuccess = {it}, onFailure = {
                throw IllegalStateException("Mottatt melding på Topic=[$topic] som ikke kan deserialisers. Melding=$maskert", it)
            })
        }
    }
}





