package no.nav.k9

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.k9.wiremock.initWireMock
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder

internal class K9PunsjApplicationWithMocks {
    internal companion object {
        internal fun startup(
                wireMockServer: WireMockServer,
                port: Int,
                args: Array<String> = arrayOf(),
                azureV2DiscoveryUrl: String? = null
        ) = SpringApplicationBuilder(K9PunsjApplication::class.java)
                .bannerMode(Banner.Mode.OFF)
                .properties(MockConfiguration.config(
                        wireMockServer = wireMockServer,
                        port = port,
                        azureV2DiscoveryUrl = azureV2DiscoveryUrl
                ))
                .main(K9PunsjApplication::class.java)
                .run(*args)

        @JvmStatic
        fun main(args: Array<String>) {
            val wireMockServer = initWireMock(
                    port = 8084
            )
            startup(
                    wireMockServer = wireMockServer,
                    port = 8085,
                    azureV2DiscoveryUrl = "http://localhost:8082/.well-known/openid-configuration"
            )
        }
    }
}
