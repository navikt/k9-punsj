package no.nav.k9punsj

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.wiremock.initWireMock
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.utility.MountableFile
import java.net.URI
import java.util.*

internal class K9PunsjApplicationWithMocks {
    internal companion object {
        private val postgreSQLContainer12 = PostgreSQLContainer12().apply {
            // Cloud SQL har wal_level = 'logical' på grunn av flagget cloudsql.logical_decoding i
            // naiserator.yaml. Vi må sette det samme lokalt for at flyway migrering skal fungere.
            withCommand("postgres", "-c", "wal_level=logical")

            // Kopierer init.sql til containeren med script for å opprette rollen k9-punsj-admin.
            withCopyFileToContainer(
                MountableFile.forClasspathResource(
                    "db/init.sql"
                ),
                "/docker-entrypoint-initdb.d/init.sql"
            )
        }

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
                        azureV2Url = azureV2Url,
                        postgresqlContainer = postgreSQLContainer12
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
                rootDirectory = "src/test/resources"
            )

            postgreSQLContainer12.start()

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    postgreSQLContainer12.close()
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

    internal class PostgresqlContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            postgreSQLContainer12.start()

            TestPropertyValues.of(
                "no.nav.db.url=${postgreSQLContainer12.jdbcUrl}",
                "no.nav.db.username=${postgreSQLContainer12.username}",
                "no.nav.db.password=${postgreSQLContainer12.password}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
