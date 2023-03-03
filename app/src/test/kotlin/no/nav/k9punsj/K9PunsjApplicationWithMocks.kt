package no.nav.k9punsj

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.PunsjJournalpost
import no.nav.k9punsj.util.DatabaseUtil
import no.nav.k9punsj.wiremock.initWireMock
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.net.URI
import java.util.UUID

internal class K9PunsjApplicationWithMocks {
    internal companion object {
        internal fun startup(
            wireMockServer: WireMockServer,
            port: Int,
            args: Array<String> = arrayOf(),
            azureV2Url: URI? = null,
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

            return builder.run(*args)
        }

        private fun lokaltKjørendeAzureV2OrNull(): URI? {
            val potensiellUrl = URI("http://localhost:8100/v2.0")
            val kjørerLokalt = runBlocking {
                val (_, response, _) = "$potensiellUrl/.well-known/openid-configuration"
                    .httpGet()
                    .timeout(200)
                    .awaitStringResponseResult()
                response.statusCode == 200
            }
            return when (kjørerLokalt) {
                true -> potensiellUrl
                false -> null
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val wireMockServer = initWireMock(
                port = 8084,
                rootDirectory = "mock-server/src/main/resources"
            )

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    DatabaseUtil.embeddedPostgres.close()
                    wireMockServer.stop()
                }
            )

            val applicationContext = startup(
                wireMockServer = wireMockServer,
                port = 8085,
                azureV2Url = lokaltKjørendeAzureV2OrNull(),
                profiles = "local"

            )
            runBlocking {
                applicationContext?.getBean(JournalpostService::class.java)?.lagre(
                    punsjJournalpost = PunsjJournalpost(
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
