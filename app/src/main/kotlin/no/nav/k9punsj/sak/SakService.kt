package no.nav.k9punsj.sak

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9punsj.journalpost.SafGateway
import org.json.JSONArray
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SakService(
    private val safGateway: SafGateway
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SakService::class.java)
    }

    suspend fun hentSaker(søkerIdent: String): List<SakInfoDto> {
        val sakerFraSaf = safGateway.hentSakerFraSaf(søkerIdent)
        return when {
            sakerFraSaf != null -> sakerFraSaf.somSakInfo()
                .filter { "oms" == it.tema.lowercase() }
                .filter { "k9" == it.fagsaksystem.lowercase() }
                .filter { "fagsak" == it.sakstype.lowercase() }
            else -> listOf()
        }
    }

    private fun JSONArray.somSakInfo(): List<SakInfoDto> {
        return try {
            jacksonObjectMapper().readValue(this.toString())
        } catch (ex: Throwable) {
            logger.error("Feilet med å deserialisere saker: {}", ex)
            throw ex
        }
    }

    data class SakInfoDto(
        val fagsakId: String? = null,
        val fagsaksystem: String,
        val sakstype: String,
        val tema: String
    )
}
