package no.nav.k9

import no.nav.k9.wiremock.initWireMock
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder

internal class K9PunsjApplicationWithMocks {
    private companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val wireMockServer = initWireMock(
                    port = 8082
            )
            SpringApplicationBuilder(K9PunsjApplication::class.java)
                    .bannerMode(Banner.Mode.OFF)
                    .properties(MockConfiguration.config(
                            wireMockServer = wireMockServer
                    ))
                    .main(K9PunsjApplication::class.java)
                    .run(*args)
        }
    }
}