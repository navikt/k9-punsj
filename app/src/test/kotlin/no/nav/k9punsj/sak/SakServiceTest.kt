package no.nav.k9punsj.sak

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.rest.eksternt.k9sak.TestK9SakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class SakServiceTest {

    private lateinit var sakService: SakService

    @MockBean
    private lateinit var personService: PersonService

    @BeforeEach
    internal fun setUp() {
        sakService = SakService(k9SakService = TestK9SakService(), personService = personService)
    }

    @Test
    internal fun `gitt saker fra saf, forvent kun fagsaker fra k9`() {
        runBlocking {
            val saker = sakService.hentSaker("123")
            assertThat(saker.isNotEmpty())
        }
    }
}
