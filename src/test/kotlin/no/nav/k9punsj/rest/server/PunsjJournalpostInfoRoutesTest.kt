package no.nav.k9punsj.rest.server

import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.felles.JsonB
import no.nav.k9punsj.journalpost.dto.SøkUferdigJournalposter
import no.nav.k9punsj.utils.objectMapper
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.BodyInserters

class PunsjJournalpostInfoRoutesTest : AbstractContainerBaseTest() {

    private val json: JsonB = objectMapper().convertValue(SøkUferdigJournalposter("1000000000000", null))

    @Test
    fun `Får en liste med journalpostIder som ikke er ferdig behandlet av punsj post`(): Unit = runBlocking {
        val k9sakToken = "Bearer ${no.nav.helse.dusseldorf.testsupport.jws.Azure.V2_0.saksbehandlerAccessToken()}"

        webTestClient.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, k9sakToken)
            .body(BodyInserters.fromValue(json))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `Http 500 om vi sender feil body`(): Unit = runBlocking {
        val k9sakToken = "Bearer ${no.nav.helse.dusseldorf.testsupport.jws.Azure.V2_0.saksbehandlerAccessToken()}"

        webTestClient.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, k9sakToken)
            .body(BodyInserters.fromValue("""json"""))
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    fun `Http 401 om vi har token fra riktig applikasjon men feil aud`(): Unit = runBlocking {
        val stsTokenLosApi = "Bearer ${
            no.nav.helse.dusseldorf.testsupport.jws.NaisSts.generateJwt(
                application = "srvk9sak",
                overridingClaims = mapOf(
                    "aud" to "k9losapi"
                )
            )
        }"
        webTestClient.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, stsTokenLosApi)
            .body(BodyInserters.fromValue(json))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `Http 401 om vi har token til annen applikasjon`(): Unit = runBlocking {
        val stsTokenLosApi = "Bearer ${
            no.nav.helse.dusseldorf.testsupport.jws.NaisSts.generateJwt(
                application = "srvk9losapi",
                overridingClaims = mapOf(
                    "sub" to "srvk9losapi",
                    "aud" to "srvk9losapi"
                )
            )
        }"
        webTestClient.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, stsTokenLosApi)
            .body(BodyInserters.fromValue(json))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `Http 401 om ikke har token`(): Unit = runBlocking {
        webTestClient.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .body(BodyInserters.fromValue(json))
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `Http 200 om vi har azure ad token`(): Unit = runBlocking {
        val azureToken = "Bearer ${no.nav.helse.dusseldorf.testsupport.jws.Azure.V2_0.saksbehandlerAccessToken()}"
        webTestClient.post().uri {
            it.pathSegment("api", "journalpost", "uferdig").build()
        }
            .header(HttpHeaders.AUTHORIZATION, azureToken)
            .body(BodyInserters.fromValue(json))
            .exchange()
            .expectStatus().isOk
    }
}
