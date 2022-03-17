package no.nav.k9punsj.journalpost

import no.nav.k9punsj.journalpost.JournalpostRoutes.Companion.hentBareKodeverdien
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class PunsjJournalpostRoutesTest {

    @Test
    internal fun testHentKodeVerdi() {
        val hentBareKodeverdien = "2970 NAV IKT DRIFT".hentBareKodeverdien()
        Assertions.assertThat(hentBareKodeverdien).isEqualTo("2970")
    }
}





