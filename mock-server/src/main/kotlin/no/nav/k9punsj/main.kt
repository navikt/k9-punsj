package no.nav.k9punsj

import no.nav.k9punsj.wiremock.initWireMock

fun main() {
    val port = 8084

    val rootDirectory = System.getenv("K9_MOCKS_ROOT_DIR") ?: "mock-server/src/main/resources"
    val wiremock = initWireMock(port, rootDirectory = rootDirectory)

    while (wiremock.isRunning) {
        Thread.sleep(1000)
    }
}
