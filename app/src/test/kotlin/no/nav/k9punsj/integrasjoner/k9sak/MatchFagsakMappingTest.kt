package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl.Companion.inneholderMatchendeFagsak
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
    }
}