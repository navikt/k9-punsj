package no.nav.k9punsj

import com.github.tomakehurst.wiremock.WireMockServer
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.journalpost.Journalpost
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.wiremock.initWireMock
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.util.*

internal class K9PunsjApplicationWithMocks {
    internal companion object {
        internal fun startup(
                wireMockServer: WireMockServer,
                port: Int,
                args: Array<String> = arrayOf(),
                azureV2Url: String? = null,
                profiles: String? = null
        ): ConfigurableApplicationContext? {
            val builder = SpringApplicationBuilder(K9PunsjApplication::class.java)
                    .bannerMode(Banner.Mode.OFF)
                    .properties(
                        MockConfiguration.config(
                            wireMockServer = wireMockServer,
                            port = port,
                            azureV2Url = azureV2Url
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

            Runtime.getRuntime().addShutdownHook(Thread {
                DatabaseUtil.embeddedPostgres.close()
                wireMockServer.stop()
            })

            val applicationContext = startup(
                    wireMockServer = wireMockServer,
                    port = 8085,
                    azureV2Url = "http://localhost:8100/v2.0",
                    profiles = "local"

            )
            runBlocking {
                applicationContext?.getBean(JournalpostService::class.java)?.lagre(
                    journalpost = Journalpost(
                        uuid = UUID.randomUUID(),
                        journalpostId = "56745674",
                        aktørId = "56745674",
                        type = "KOPI"
                    )
                )
            }
        }
    }
}
