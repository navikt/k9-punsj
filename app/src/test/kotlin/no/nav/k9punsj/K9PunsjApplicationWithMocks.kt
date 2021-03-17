package no.nav.k9punsj

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.k9punsj.wiremock.initWireMock
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext

internal class K9PunsjApplicationWithMocks {
    internal companion object {
        internal fun startup(
                wireMockServer: WireMockServer,
                port: Int,
                args: Array<String> = arrayOf(),
                azureV2DiscoveryUrl: String? = null,
                profiles: String? = null
        ): ConfigurableApplicationContext? {
            val builder = SpringApplicationBuilder(K9PunsjApplication::class.java)
                    .bannerMode(Banner.Mode.OFF)
                    .properties(
                        MockConfiguration.config(
                            wireMockServer = wireMockServer,
                            port = port,
                            azureV2DiscoveryUrl = azureV2DiscoveryUrl
                        )
                    )
                    .main(K9PunsjApplication::class.java)

            if (profiles != null) {
                builder.profiles(profiles)
            }

            return builder
                    .run(*args)
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val wireMockServer = initWireMock(
                    port = 8084,
                    rootDirectory = "mock-server/src/main/resources"
            )
            startup(
                    wireMockServer = wireMockServer,
                    port = 8085,
                    azureV2DiscoveryUrl = "http://azure-mock:8100/v2.0/.well-known/openid-configuration",
                    profiles = "local"

            )
        }
    }
}
