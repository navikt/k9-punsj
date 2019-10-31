package no.nav.k9

import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder

internal class K9PunsjApplicationWithMocks {
    private companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(K9PunsjApplication::class.java)
                    .bannerMode(Banner.Mode.OFF)
                    .properties(MockConfiguration.config())
                    .main(K9PunsjApplication::class.java)
                    .run(*args)
        }
    }
}