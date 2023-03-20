package no.nav.k9punsj.integrasjoner.k9sak

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MatchFagsakMappingTest {

    @Test
    fun `mapper matching av fagsaker`() {
        assertTrue(HarFagsaker.inneholderMatchendeFagsak())
        assertFalse(IngenFagsaker.inneholderMatchendeFagsak())
        assertFalse(KunOpprettetFagsaker.inneholderMatchendeFagsak())
    }

    private companion object {
        private const val IngenFagsaker = "[]"
        private const val HarFagsaker = """[{"status": "bar"},{"status":"bar2"}]"""
        private const val KunOpprettetFagsaker = """[{"status":"OPPR"}]"""

        fun String.inneholderMatchendeFagsak() = JSONArray(this)
            .asSequence()
            .map { it as JSONObject }
            .filterNot { it.getString("status") == "OPPR" }
            .toSet()
            .isNotEmpty()
    }
}