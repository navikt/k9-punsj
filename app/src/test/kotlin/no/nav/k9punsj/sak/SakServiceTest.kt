package no.nav.k9punsj.sak

import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.journalpost.SafGateway
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class SakServiceTest {

    @MockK
    private lateinit var safGateway: SafGateway

    @InjectMockKs
    private lateinit var sakService: SakService

    @Test
    internal fun `gitt jsonArray fra saf, forvent korrekt mapping`() {
        coEvery { safGateway.hentSakerFraSaf(any()) } returns JSONArray(
            //language=json
            """
                 [
                      {
                        "fagsakId": "10695768",
                        "fagsaksystem": "AO01",
                        "sakstype": "FAGSAK",
                        "tema": "OMS"
                      },
                      {
                        "fagsakId": "1DMU93M",
                        "fagsaksystem": "K9",
                        "sakstype": "FAGSAK",
                        "tema": "OMS"
                      },
                      {
                        "fagsakId": "1DMUDF6",
                        "fagsaksystem": "K9",
                        "sakstype": "FAGSAK",
                        "tema": "OMS"
                      }
                ]
            """.trimIndent()
        )

        runBlocking {
            val saker = sakService.hentSaker("123")
            assertThat(saker).size().isEqualTo(3)
        }
    }
}
