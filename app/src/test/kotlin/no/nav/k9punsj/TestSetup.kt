package no.nav.k9punsj

import no.nav.k9punsj.wiremock.initWireMock
import org.springframework.web.reactive.function.client.WebClient

object TestSetup {
    val wireMockServer = initWireMock(
            port = 9192
    )

    private const val port = 9194

    val client = WebClient.create("http://localhost:$port/")

    private val app = K9PunsjApplicationWithMocks.startup(
        wireMockServer = wireMockServer,
        port = port,
        profiles = "test"
    )
}
