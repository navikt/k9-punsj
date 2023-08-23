package no.nav.k9punsj.felles

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SakTest {

    @Test
    fun `gitt at sakstype er fagsak, forvent at fagsaksystem er K9`() {
        val sak = Sak(sakstype = Sak.SaksType.FAGSAK, fagsakId = "ABC123")
        assertThat(sak.fagsaksystem).isNotNull.isEqualTo(Sak.FagsakSystem.K9)
    }
}
