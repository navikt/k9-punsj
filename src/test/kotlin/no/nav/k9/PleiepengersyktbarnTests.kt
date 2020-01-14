package no.nav.k9

import kotlinx.coroutines.runBlocking
import no.nav.k9.mappe.MapperSvarDTO
import no.nav.k9.wiremock.initWireMock
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@ExtendWith(SpringExtension::class)
class K9PunsjPleiepengerTests {

    private companion object {
        private val wireMockServer = initWireMock(
                port = 9192
        )

        private const val port = 9193
        private val client = WebClient.create("http://localhost:$port/api/pleiepenger-sykt-barn-soknad/")

        private val app = K9PunsjApplicationWithMocks.startup(
                wireMockServer = wireMockServer,
                port = port
        )

        @JvmStatic
        @AfterAll
        fun tearDown() {
            wireMockServer.stop()
            app.stop()
        }
    }

    @Test
    fun `Hente eksisterende mapper`() {
        val res = client.get().uri{ it.pathSegment("mapper").build() }
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())
    }

    @Test
    fun `Opprette ny mappe uten person`() {
        val innsending = Innsending(personer = mutableMapOf())
        val res = client.post()
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, res.statusCode())
    }

    @Test
    fun `Opprette ny mappe på person`() {
        val innsending = lagInnsending("12345", "999")
        val res = client.post()
                .body(BodyInserters.fromValue(innsending))
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, res.statusCode())
    }

    @Test
    fun `Hente eksisterende mappe på person`() {
        val innsending = lagInnsending("12345", "9999")

        val resPost = client.post().body(BodyInserters.fromValue(innsending)).awaitExchangeBlocking()
        assertEquals(HttpStatus.CREATED, resPost.statusCode())

        val res = client.get().uri{ it.pathSegment("mapper").build() }
                .header("X-Nav-NorskIdent", "12345")
                .awaitExchangeBlocking()
        assertEquals(HttpStatus.OK, res.statusCode())

        val mapperSvar = runBlocking { res.awaitBody<MapperSvarDTO>() }
        val personerSvar = mapperSvar.mapper.first().personer["12345"]
        assertEquals("9999", personerSvar?.innsendinger?.first())
    }
}

private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }


private fun lagInnsending(personnummer: NorskIdent, journalpostId: String): Innsending {
    val person = JournalpostInnhold<SøknadJson>(journalpostId = journalpostId, soeknad = mutableMapOf())
    val personer = mutableMapOf<String, JournalpostInnhold<SøknadJson>>()
    personer[personnummer] = person

    val innsending = Innsending(personer)
    return innsending
}