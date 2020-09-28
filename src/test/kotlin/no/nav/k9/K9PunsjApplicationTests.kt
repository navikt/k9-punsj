package no.nav.k9

import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9.mappe.MappeSvarDTO
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
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.core.publisher.Mono

@ExtendWith(SpringExtension::class)
@TestPropertySource(locations = ["classpath:application.yml"])
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

		assertEquals(HttpStatus.OK, res.statusCode())
	}

	@Test
	fun `Hente et dokument fra Journalpost uten credentials feiler`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", "1", "dokument", "1").build()
		}.awaitExchangeBlocking()

		assertEquals(HttpStatus.UNAUTHORIZED, res.statusCode())
	}

	@Test
	fun `Hente et dokument fra Journalpost fungerer`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.Ok, "dokument", "1").build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(HttpStatus.OK, res.statusCode())
		val responsePdf = runBlocking { res.awaitBody<ByteArray>() }
		assertArrayEquals(responsePdf, dummyPdf)
	}

	@Test
	fun `Hente et dokument fra Journalpost som ikke finnes håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke, "dokument", "1").build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(HttpStatus.NOT_FOUND, res.statusCode())
	}

	@Test
	fun `Hente et dokument fra Journalpost uten tilgang håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.AbacError, "dokument", "1").build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(HttpStatus.FORBIDDEN, res.statusCode())
	}

	@Test
	fun `Hente journalpostinfo fungerer`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", "1").build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()
		val responseEntity = runBlocking { res.awaitBody<String>() }
		JSONAssert.assertEquals("""
			{
				"journalpostId": "1",
				"norskIdent": "29099012345",
				"dokumenter": [{
					"dokumentId": "470164680"
				},{
					"dokumentId": "470164681"
				}]
			}
		""".trimIndent(),responseEntity, true)
	}

	@Test
	fun `Hente journalpostinfo for ikke eksisterende journalpost håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke).build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(HttpStatus.NOT_FOUND, res.statusCode())
	}

	@Test
	fun `Hente journalpostinfo på journalpost uten tilgang på journalpostnivå håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.AbacError).build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(HttpStatus.FORBIDDEN, res.statusCode())
	}

	@Test
	fun `Hente journalpostinfo på journalpost uten tilgang på alle dokumenter håndteres`() {
		val res= client.get().uri {
			it.pathSegment("api", "journalpost", JournalpostIds.IkkeKomplettTilgang).build()
		}.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

		assertEquals(HttpStatus.FORBIDDEN, res.statusCode())
	}

	@Test
	fun `Crud-test`() {

		// Opprette mappe
		val body = BodyInserters.fromPublisher(
				Mono.just(Innsending(personer = mapOf(Pair("15049228314", JournalpostInnhold(
						journalpostId = "200",
						soeknad = mutableMapOf()
				))))),
				Innsending::class.java
		)
		val resOpprette = client
				.post()
				.uri { it.pathSegment("api", "pleiepenger-sykt-barn-soknad").build() }
				.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(body)
				.awaitExchangeBlocking()
		assertEquals(HttpStatus.CREATED, resOpprette.statusCode())
		val opprettetMappe = resOpprette
				.bodyToMono(MappeSvarDTO::class.java)
				.block()
		val mappeid = opprettetMappe?.mappeId

		// Finne opprettet mappe
		val resFinneMappe = client
				.get()
				.uri { it.pathSegment("api", "mappe", mappeid).build() }
				.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
				.awaitExchangeBlocking()
		assertEquals(HttpStatus.OK, resFinneMappe.statusCode())
		val funnetMappe = resFinneMappe
				.bodyToMono(MappeSvarDTO::class.java)
				.block()
		assertEquals(funnetMappe?.mappeId, mappeid)

		// TODO: Oppdatere mappe
		// TODO: Finne oppdatert mappe og verifisere at den er oppdatert
		// TODO: Slette mappe
		// TODO: Prøve å finne slettet mappe og verifisere at den ikke finnes
	}

	@Test
	fun databasetest() {

		assert(true);
	}
}

private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }
