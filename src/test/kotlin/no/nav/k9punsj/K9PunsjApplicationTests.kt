package no.nav.k9punsj

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.wiremock.JournalpostIds
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders

class K9PunsjApplicationTests : AbstractContainerBaseTest() {

    private val dummyPdf = ClassPathResource("__files/dummy_soknad.pdf").inputStream.readBytes()

    @Test
    fun `Endepunkt brukt for isReady og isAlive fungerer`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/internal/actuator/health").build() }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
                """
                {
                    "status": "UP"
                }
            """.trimIndent()
            )
    }

    @Test
    fun `Hente et dokument fra Journalpost uten credentials feiler`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/api/journalpost/1/dokument/1").build() }
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `Hente et dokument fra Journalpost fungerer`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/api/journalpost/${JournalpostIds.Ok}/dokument/1").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith {
                assertArrayEquals(it.responseBody, dummyPdf)
            }
    }

    @Test
    fun `Hente et dokument fra Journalpost som ikke finnes håndteres`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/api/journalpost/${JournalpostIds.FinnesIkke}/dokument/1").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Hente et dokument fra Journalpost uten tilgang håndteres`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/api/journalpost/${JournalpostIds.AbacError}/dokument/1").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `Hente journalpostinfo fungerer`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/api/journalpost/1").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
                """
                {
                    "journalpostId": "1",
                    "norskIdent": "29099000129",
                    "dokumenter": [
                        {
                            "dokumentId": "470164680"
                        },
                        {
                            "dokumentId": "470164681"
                        }
                    ],
                    "venter": null,
                    "punsjInnsendingType": null,
                    "kanSendeInn": true,
                    "erSaksbehandler": true,
                    "journalpostStatus": "MOTTATT",
                    "kanOpprettesJournalføringsoppgave": true,
                    "kanKopieres": true,
                    "gosysoppgaveId": null,
                    "erFerdigstilt": false
                }
            """.trimIndent()
            )
    }

    @Test
    fun `Hente journalpostinfo for ikke eksisterende journalpost håndteres`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/api/journalpost/${JournalpostIds.FinnesIkke}").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Hente journalpostinfo på journalpost uten tilgang på journalpostnivå håndteres`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/api/journalpost/${JournalpostIds.AbacError}").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `Hente journalpostinfo på journalpost uten tilgang på alle dokumenter håndteres`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/api/journalpost/${JournalpostIds.IkkeKomplettTilgang}").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `journalposter med status ferdigstilt eller journalfort for erFerdigstilt true`(): Unit = runBlocking {
        webTestClient.get()
            .uri { it.path("/api/journalpost/7523521").build() }
            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
                """
                {
                    "journalpostId": "7523521",
                    "norskIdent": "02020050123",
                    "dokumenter": [
                        {
                            "dokumentId": "470164680"
                        },
                    ],
                    "venter": null,
                    "punsjInnsendingType": null,
                    "kanSendeInn": true,
                    "erSaksbehandler": true,
                    "journalpostStatus": "FERDIGSTILT",
                    "kanOpprettesJournalføringsoppgave": false,
                    "kanKopieres": false,
                    "gosysoppgaveId": null,
                    "erFerdigstilt": true
                }
            """.trimIndent()
            )
    }
}
