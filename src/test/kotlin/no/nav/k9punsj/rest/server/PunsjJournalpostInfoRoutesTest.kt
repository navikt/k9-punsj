package no.nav.k9punsj.rest.server

import com.fasterxml.jackson.module.kotlin.convertValue
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.felles.JsonB
import no.nav.k9punsj.journalpost.dto.SøkUferdigJournalposter
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.util.WebClientUtils.awaitStatuscode
import no.nav.k9punsj.wiremock.k9SakToken
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.BodyInserters

@ExtendWith(SpringExtension::class, MockKExtension::class)
class PunsjJournalpostInfoRoutesTest {

    private val client = TestSetup.client
    private val json: JsonB = objectMapper().convertValue(SøkUferdigJournalposter("1000000000000", null))

    @Test
    fun `Får en liste med journalpostIder som ikke er ferdig behandlet av punsj post`(): Unit = runBlocking {
        val k9sakToken = "Bearer ${no.nav.helse.dusseldorf.testsupport.jws.NaisSts.k9SakToken()}"
        val res = client.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, k9sakToken)
            .body(BodyInserters.fromValue(json))

        val status = res.awaitStatuscode()
        assertEquals(HttpStatus.OK, status)
    }

    @Test
    fun `Http 500 om vi sender feil body`(): Unit = runBlocking {
        val k9sakToken = "Bearer ${no.nav.helse.dusseldorf.testsupport.jws.NaisSts.k9SakToken()}"
        val res = client.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, k9sakToken)
            .body(BodyInserters.fromValue("""json"""))

        val status = res.awaitStatuscode()
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
    }

    @Test
    fun `Http 401 om vi har token fra riktig applikasjon men feil aud`() = runBlocking {
        val stsTokenLosApi = "Bearer ${no.nav.helse.dusseldorf.testsupport.jws.NaisSts.generateJwt(
            application = "srvk9sak",
            overridingClaims = mapOf(
                "aud" to "k9losapi"
            ))}"
        val res = client.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, stsTokenLosApi)
            .body(BodyInserters.fromValue(json))

        val status = res.awaitStatuscode()
        assertEquals(HttpStatus.UNAUTHORIZED, status)
    }

    @Test
    fun `Http 401 om vi har token til annen applikasjon`() = runBlocking {
        val stsTokenLosApi = "Bearer ${no.nav.helse.dusseldorf.testsupport.jws.NaisSts.generateJwt(
            application = "srvk9losapi",
            overridingClaims = mapOf(
                "sub" to "srvk9losapi",
                "aud" to "srvk9losapi"
            ))}"
        val res = client.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, stsTokenLosApi)
            .body(BodyInserters.fromValue(json))

        val status = res.awaitStatuscode()
        assertEquals(HttpStatus.UNAUTHORIZED, status)
    }

    @Test
    fun `Http 401 om ikke har token`() = runBlocking {
        val res = client.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .body(BodyInserters.fromValue(json))

        val status = res.awaitStatuscode()
        assertEquals(HttpStatus.UNAUTHORIZED, status)
    }

    @Test
    fun `Http 200 om vi har azure ad token`() = runBlocking {
        val azureToken = "Bearer ${no.nav.helse.dusseldorf.testsupport.jws.Azure.V2_0.saksbehandlerAccessToken()}"
        val res = client.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, azureToken)
            .body(BodyInserters.fromValue(json))

        val status = res.awaitStatuscode()
        assertEquals(HttpStatus.OK, status)
    }
}
