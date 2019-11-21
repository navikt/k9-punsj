package no.nav.k9

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.wiremock.initWireMock
import no.nav.k9.wiremock.saksbehandlerAccessToken
import org.junit.AfterClass
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.awaitBody

@RunWith(SpringRunner::class)
class K9PunsjApplicationTests {

	private companion object {

		private val wireMockServer = initWireMock(
				port = 8084
		)

		private const val port = 8083
		private val client = WebClient.create("http://localhost:$port")

		private val app = K9PunsjApplicationWithMocks.startup(
				wireMockServer = wireMockServer,
				port = port
		)

		private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"
		private val dummyPdf = ClassPathResource("__files/dummy_soknad.pdf").inputStream.readBytes()

		@JvmStatic
		@AfterClass
		fun tearDown() {
			wireMockServer.stop()
			app.stop()
		}

	}

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
	fun `Hente et dokument fra Journalpost med credentials fungerer`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", "1", "dokument", "1").build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
				.awaitExchangeBlocking()

		assertEquals(res.statusCode(), HttpStatus.OK)
		val responsePdf = runBlocking { res.awaitBody<ByteArray>() }
		assertArrayEquals(responsePdf, dummyPdf)
	}
}

private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }
