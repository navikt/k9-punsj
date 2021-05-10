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
    fun `Endepunkt brukt for isReady og isAlive fungerer`() {
        val res =
            client.get().uri {
                it.pathSegment("internal", "actuator", "info").build()
            }.awaitExchangeBlocking()

        assertEquals(HttpStatus.OK, res.statusCode())
    }

    @Test
    fun `Hente et dokument fra Journalpost uten credentials feiler`() {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", "1", "dokument", "1").build()
        }.awaitExchangeBlocking()

        assertEquals(HttpStatus.UNAUTHORIZED, res.statusCode())
    }

    @Test
    fun `Hente et dokument fra Journalpost fungerer`() {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.Ok, "dokument", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

        assertEquals(HttpStatus.OK, res.statusCode())
        val responsePdf = runBlocking { res.awaitBody<ByteArray>() }
        assertArrayEquals(responsePdf, dummyPdf)
    }

    @Test
    fun `Hente et dokument fra Journalpost som ikke finnes håndteres`() {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke, "dokument", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

        assertEquals(HttpStatus.NOT_FOUND, res.statusCode())
    }

    @Test
    fun `Hente et dokument fra Journalpost uten tilgang håndteres`() {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.AbacError, "dokument", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

        assertEquals(HttpStatus.FORBIDDEN, res.statusCode())
    }

    @Test
    fun `Hente journalpostinfo fungerer`() {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", "1").build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()
        val responseEntity = runBlocking { res.awaitBody<String>() }
        JSONAssert.assertEquals("""
			{
				"journalpostId": "1",
				"norskIdent": "29099000129",
				"dokumenter": [{
					"dokumentId": "470164680"
				},{
					"dokumentId": "470164681"
				}],
                "venter" : null
			}
		""".trimIndent(), responseEntity, true)
    }

    @Test
    fun `Hente journalpostinfo for ikke eksisterende journalpost håndteres`() {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.FinnesIkke).build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

        assertEquals(HttpStatus.NOT_FOUND, res.statusCode())
    }

    @Test
    fun `Hente journalpostinfo på journalpost uten tilgang på journalpostnivå håndteres`() {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.AbacError).build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

        assertEquals(HttpStatus.FORBIDDEN, res.statusCode())
    }

    @Test
    fun `Hente journalpostinfo på journalpost uten tilgang på alle dokumenter håndteres`() {
        val res = client.get().uri {
            it.pathSegment("api", "journalpost", JournalpostIds.IkkeKomplettTilgang).build()
        }.header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader).awaitExchangeBlocking()

        assertEquals(HttpStatus.FORBIDDEN, res.statusCode())
    }

//    @Test
//    fun `Crud-test for mapper`() {
//
//        // Opprette mappe for person og lagre søknad
//        val soknad = genererSoknad()
//        val body = genererBody(soknad)
//        val resOpprette = client
//            .post()
//            .uri { it.pathSegment("api", "pleiepenger-sykt-barn-soknad").build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//            .body(body)
//            .awaitExchangeBlocking()
//        assertEquals(HttpStatus.CREATED, resOpprette.statusCode())
//        val opprettetMappe = resOpprette
//            .bodyToMono(MappeSvarDTO::class.java)
//            .block()
//        val mappeid = opprettetMappe?.mappeId
//
//        // Finne opprettet mappe
//        val resFinneMappe = client
//            .get()
//            .uri { it.pathSegment("api", "pleiepenger-sykt-barn-soknad", "mappe", mappeid).build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .awaitExchangeBlocking()
//        assertEquals(HttpStatus.OK, resFinneMappe.statusCode())
//        val funnetMappe = resFinneMappe
//            .bodyToMono(MappeSvarDTO::class.java)
//            .block()
//        assertEquals(funnetMappe?.mappeId, mappeid)
//
//        // Oppdatere mappe
//        val nyttSpråk = Språk.NORSK_NYNORSK
//        val oppdatertSoknad = genererSoknad(spraak = nyttSpråk)
//        val oppdatertBody = genererBody(oppdatertSoknad)
//        val resOppdatereMappe = client
//            .put()
//            .uri { it.pathSegment("api", "pleiepenger-sykt-barn-soknad", "mappe", mappeid).build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//            .body(oppdatertBody)
//            .awaitExchangeBlocking()
//        assertEquals(HttpStatus.OK, resOppdatereMappe.statusCode())
//
//        // Finne oppdatert mappe og verifisere at den er oppdatert
//        val resFinneOppdatertMappe = client
//            .get()
//            .uri { it.pathSegment("api", "pleiepenger-sykt-barn-soknad", "mappe", mappeid).build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .awaitExchangeBlocking()
//        assertEquals(HttpStatus.OK, resFinneOppdatertMappe.statusCode())
//        val funnetOppdatertSoknad = resFinneOppdatertMappe
//            .bodyToMono(MappeSvarDTO::class.java)
//            .block()
//        assertEquals(nyttSpråk.name, funnetOppdatertSoknad?.personer?.get(standardIdent)?.soeknad?.get("spraak"))
//
//        // Slette mappe
//        val resSletteMappe = client
//            .delete()
//            .uri { it.pathSegment("api", "pleiepenger-sykt-barn-soknad", "mappe", mappeid).build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .awaitExchangeBlocking()
//        assertEquals(HttpStatus.NO_CONTENT, resSletteMappe.statusCode())
//
//        // Prøve å finne slettet mappe og verifisere at den ikke finnes
//        val resFinneSlettetMappe = client
//            .get()
//            .uri { it.pathSegment("api", "pleiepenger-sykt-barn-soknad", "mappe", mappeid).build() }
//            .header(HttpHeaders.AUTHORIZATION, saksbehandlerAuthorizationHeader)
//            .awaitExchangeBlocking()
//        assertEquals(HttpStatus.NOT_FOUND, resFinneSlettetMappe.statusCode())
//    }
//}
//
//private fun genererBody(
//    soknad: SøknadJson,
//    ident: NorskIdent = standardIdent,
//): BodyInserter<Mono<Innsending>, ReactiveHttpOutputMessage> {
//    return BodyInserters.fromPublisher(
//        Mono.just(Innsending(personer = mapOf(Pair(ident, JournalpostInnhold(
//            journalpostId = "200",
//            soeknad = soknad
//        ))))),
//        Innsending::class.java
//    )
}


private fun WebClient.RequestHeadersSpec<*>.awaitExchangeBlocking(): ClientResponse = runBlocking { awaitExchange() }
