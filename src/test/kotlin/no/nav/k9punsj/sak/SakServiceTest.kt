package no.nav.k9punsj.sak

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.domenetjenester.PersonService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

internal class SakServiceTest: AbstractContainerBaseTest() {

    @Autowired
    private lateinit var sakService: SakService

    @MockitoBean
    private lateinit var personService: PersonService

    @Test
    internal fun `gitt saker fra saf, forvent kun fagsaker fra k9`() {
        runBlocking {
            val saker = sakService.hentSaker("123")
            assertThat(saker.isNotEmpty())
        }
    }
}
