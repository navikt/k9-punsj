package no.nav.k9punsj.sak

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.rest.eksternt.k9sak.TestK9SakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class SakServiceTest {

    private lateinit var sakService: SakService

    @BeforeEach
    internal fun setUp() {
        sakService = SakService(TestK9SakService())
    }

    @Test
    internal fun `gitt saker fra saf, forvent kun fagsaker fra k9`() {
        runBlocking {
            val saker = sakService.hentSaker("123")
            assertThat(saker.statusCode().is2xxSuccessful)
        }
    }
}
