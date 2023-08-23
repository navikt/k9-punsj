package no.nav.k9punsj

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.util.WebClientUtils.awaitBodyWithType
import no.nav.k9punsj.util.WebClientUtils.awaitStatusWithBody
import no.nav.k9punsj.util.WebClientUtils.awaitStatuscode
import no.nav.k9punsj.wiremock.JournalpostIds
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

class K9PunsjApplicationTests {

    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    private val dummyPdf = ClassPathResource("__files/dummy_soknad.pdf").inputStream.readBytes()

    val client = TestSetup.client

    @Test
    fun `Endepunkt brukt for isReady og isAlive fungerer`(): Unit = runBlocking {
        val httpStatus =
            client.get().uri {
                it.pathSegment("internal", "actuator", "info").build()
            }.awaitStatuscode()

        assertEquals(HttpStatus.OK, httpStatus)
    }

    @Test
    fun `Hente et dokument fra Journalpost uten credentials feiler`(): Unit = runBlocking {
        val httpStatus = client.get().uri {
            it.pathSegment("api", "journalpost", "1", "dokument", "1").build()
        }.awaitStatuscode()

        assertEquals(HttpStatus.UNAUTHORIZED, httpStatus)
    }

    @Test
    fun `Hente et dokument fra Journalpost fungerer`(): Unit = runBlocking {
        val (httpStatus, body) = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.Ok, "dokument", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatusWithBody<ByteArray>()

        assertEquals(HttpStatus.OK, httpStatus)
        assertArrayEquals(body, dummyPdf)
    }

    @Test
    fun `Hente et dokument fra Journalpost som ikke finnes håndteres`(): Unit = runBlocking {
        val httpStatus = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke, "dokument", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.NOT_FOUND, httpStatus)
    }

    @Test
    fun `Hente et dokument fra Journalpost uten tilgang håndteres`(): Unit = runBlocking {
        val httpStatus = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.AbacError, "dokument", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.FORBIDDEN, httpStatus)
    }

    @Test
    fun `Hente journalpostinfo fungerer`(): Unit = runBlocking {
        val body: String = client.get().uri {
            it.pathSegment("api", "journalpost", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitBodyWithType()
        JSONAssert.assertEquals(
            """{
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
            "gosysoppgaveId": null
        }
            """.trimIndent(),
            body,
            true
        )
    }

    @Test
    fun `Hente journalpostinfo for ikke eksisterende journalpost håndteres`(): Unit = runBlocking {
        val httpStatus = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke).build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.NOT_FOUND, httpStatus)
    }

    @Test
    fun `Hente journalpostinfo på journalpost uten tilgang på journalpostnivå håndteres`(): Unit = runBlocking {
        val httpStatus = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.AbacError).build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.FORBIDDEN, httpStatus)
    }

    @Test
    fun `Hente journalpostinfo på journalpost uten tilgang på alle dokumenter håndteres`(): Unit = runBlocking {
        val httpStatus = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.IkkeKomplettTilgang).build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.FORBIDDEN, httpStatus)
    }
}
