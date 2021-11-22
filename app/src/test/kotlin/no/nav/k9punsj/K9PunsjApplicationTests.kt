package no.nav.k9punsj

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.db.datamodell.Periode
import no.nav.k9punsj.wiremock.JournalpostIds
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@TestPropertySource(locations = ["classpath:application.yml"])
class K9PunsjApplicationTests {

    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
    private val dummyPdf = ClassPathResource("__files/dummy_soknad.pdf").inputStream.readBytes()

    // Standardverdier for test
    private val standardIdent = "01122334410"
    private val standardFraOgMed: LocalDate = LocalDate.of(2020, 3, 1)
    private val standardTilOgMed: LocalDate = LocalDate.of(2020, 3, 31)
    private val standardPeriode: Periode = Periode(standardFraOgMed, standardTilOgMed)
    private val standardBarnetsIdent: String = "29022050115"

    val client = TestSetup.client

    @Test
    fun `Endepunkt brukt for isReady og isAlive fungerer`() : Unit = runBlocking {
        val httpStatus =
            client.get().uri {
                it.pathSegment("internal", "actuator", "info").build()
            }.awaitStatuscode()

        assertEquals(HttpStatus.OK, httpStatus)
    }

    @Test
    fun `Hente et dokument fra Journalpost uten credentials feiler`() : Unit = runBlocking {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", "1", "dokument", "1").build()
        }.awaitStatuscode()

        assertEquals(HttpStatus.UNAUTHORIZED, res)
    }

    @Test
    fun `Hente et dokument fra Journalpost fungerer`() : Unit = runBlocking {
        val res : Pair<HttpStatus, ByteArray> = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.Ok, "dokument", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatusWithBody()

        assertEquals(HttpStatus.OK, res.first)
        assertArrayEquals(res.second, dummyPdf)
    }

    @Test
    fun `Hente et dokument fra Journalpost som ikke finnes håndteres`() : Unit = runBlocking {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke, "dokument", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.NOT_FOUND, res)
    }

    @Test
    fun `Hente et dokument fra Journalpost uten tilgang håndteres`() : Unit = runBlocking {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.AbacError, "dokument", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.FORBIDDEN, res)
    }

    @Test
    fun `Hente journalpostinfo fungerer`() : Unit = runBlocking {
        val body = client.get().uri {
            it.pathSegment("api", "journalpost", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitBodyString()
        JSONAssert.assertEquals("""{
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
	"kanKopieres": true
}""".trimIndent(), body, true)
    }

    @Test
    fun `Hente journalpostinfo for ikke eksisterende journalpost håndteres`() : Unit = runBlocking {
        val httpStatus = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke).build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.NOT_FOUND, httpStatus)
    }

    @Test
    fun `Hente journalpostinfo på journalpost uten tilgang på journalpostnivå håndteres`() : Unit = runBlocking {
        val httpStatus = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.AbacError).build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.FORBIDDEN, httpStatus)
    }

    @Test
    fun `Hente journalpostinfo på journalpost uten tilgang på alle dokumenter håndteres`() : Unit = runBlocking {
        val httpStatus = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.IkkeKomplettTilgang).build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitStatuscode()

        assertEquals(HttpStatus.FORBIDDEN, httpStatus)
    }

}


suspend fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse {
    return awaitExchange { it }
}

suspend inline fun <reified T> WebClient.RequestHeadersSpec<*>.awaitStatusWithBody(): Pair<HttpStatus, T> {
    return awaitExchange { Pair(it.statusCode(), it.awaitBody()) }
}

suspend fun WebClient.RequestHeadersSpec<*>.awaitBodyString(): String {
    return awaitExchange { it.awaitBody() }
}

suspend fun WebClient.RequestHeadersSpec<*>.awaitStatuscode(): HttpStatus {
    return awaitExchange { it.statusCode() }
}
