package no.nav.k9

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.wiremock.JournalpostIds
import no.nav.k9.wiremock.saksbehandlerAccessToken
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@ExtendWith(SpringExtension::class)
class K9PunsjApplicationTests {

	private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
	private val dummyPdf = ClassPathResource("__files/dummy_soknad.pdf").inputStream.readBytes()

	val client = TestSetup.client

	@Test
	fun `Endepunkt brukt for isReady og isAlive fungerer`() {
		val res =
		client.get().uri {
			it.pathSegment("internal", "actuator", "info").build()
		}.awaitExchangeBlocking()

		assertEquals(res.statusCode(), HttpStatus.OK)
	}

	@Test
	fun `Hente et dokument fra Journalpost uten credentials feiler`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", "1", "dokument", "1").build()
		}.awaitExchangeBlocking()

		assertEquals(res.statusCode(), HttpStatus.UNAUTHORIZED)
	}

	@Test
	fun `Hente et dokument fra Journalpost fungerer`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.Ok, "dokument", "1").build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(res.statusCode(), HttpStatus.OK)
		val responsePdf = runBlocking { res.awaitBody<ByteArray>() }
		assertArrayEquals(responsePdf, dummyPdf)
	}

	@Test
	fun `Hente et dokument fra Journalpost som ikke finnes håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke, "dokument", "1").build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(res.statusCode(), HttpStatus.NOT_FOUND)
	}

	@Test
	fun `Hente et dokument fra Journalpost uten tilgang håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.AbacError, "dokument", "1").build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(res.statusCode(), HttpStatus.FORBIDDEN)
	}

	@Test
	fun `Hente journalpostinfo fungerer`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", "1").build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()
		val responseEntity = runBlocking { res.awaitBody<String>() }
		JSONAssert.assertEquals(responseEntity, """
			{
				"journalpostId": "1",
				"norskIdent": "29099012345",
				"dokumenter": [{
					"dokumentId": "470164680"
				},{
					"dokumentId": "470164681"
				}]
			}
		""".trimIndent(), true)
	}

	@Test
	fun `Hente journalpostinfo for ikke eksisterende journalpost håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke).build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(res.statusCode(), HttpStatus.NOT_FOUND)
	}

	@Test
	fun `Hente journalpostinfo på journalpost uten tilgang på journalpostnivå håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.AbacError).build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(res.statusCode(), HttpStatus.FORBIDDEN)
	}

	@Test
	fun `Hente journalpostinfo på journalpost uten tilgang på alle dokumenter håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.IkkeKomplettTilgang).build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(res.statusCode(), HttpStatus.FORBIDDEN)
	}
}

private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }
