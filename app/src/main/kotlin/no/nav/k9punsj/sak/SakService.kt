package no.nav.k9punsj.sak

import no.nav.k9punsj.journalpost.SafGateway
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.stereotype.Service

@Service
class SakService(
    private val safGateway: SafGateway
) {

    suspend fun hentSaker(søkerIdent: String): List<SakInfo> {
        val sakerFraSaf: JSONArray? = safGateway.hentSakerFraSaf(søkerIdent)
        return when {
            sakerFraSaf != null -> {
                sakerFraSaf
                    .mapIndexed { index: Int, _ -> sakerFraSaf.getJSONObject(index).somSakInfo() }
                    .filter { "OMS" === it.tema }
            }
            else -> listOf()
        }
    }

    fun JSONObject.somSakInfo(): SakInfo {
        val fagsakId = getString("fagsakId")
        val fagsaksystem = getString("fagsaksystem")
        val sakstype = getString("sakstype")
        val tema = getString("tema")

        return SakInfo(
            fagsakId = fagsakId,
            fagsaksystem = fagsaksystem,
            sakstype = sakstype,
            tema = tema
        )
    }

    data class SakInfo(
        val fagsakId: String,
        val fagsaksystem: String,
        val sakstype: String,
        val tema: String
    )
}
