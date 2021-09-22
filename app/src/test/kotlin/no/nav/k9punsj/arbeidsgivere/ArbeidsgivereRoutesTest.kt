package no.nav.k9punsj.arbeidsgivere

import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.TestSetup
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange

@ExtendWith(SpringExtension::class, MockKExtension::class)
internal class ArbeidsgivereRoutesTest {

    private val client = TestSetup.client
    private val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"


    private fun String.get(): Pair<HttpStatus, String?> = this.let { path -> runBlocking {
        client.get()
            .uri { it.path(path).build() }
            .accept(MediaType.APPLICATION_JSON)
            .awaitExchange { it.statusCode() to it.awaitBodyOrNull() }
    }}

}