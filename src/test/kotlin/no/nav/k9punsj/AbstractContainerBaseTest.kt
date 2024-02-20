package no.nav.k9punsj

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.wiremock.initWireMock
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.MountableFile
import java.net.URI


class PostgreSQLContainer12 : PostgreSQLContainer<PostgreSQLContainer12>("postgres:12")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@SpringBootTest(
    classes = [K9PunsjApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@ContextConfiguration(initializers = [AbstractContainerBaseTest.Initializer::class])
@ExtendWith(SpringExtension::class)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@EmbeddedKafka
abstract class AbstractContainerBaseTest {

    lateinit var wireMockServer: WireMockServer

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    companion object {
        val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

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

        fun lokaltKjørendeAzureV2OrNull(): URI? {
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
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            postgreSQLContainer12.start()

            TestPropertyValues.of(
                "no.nav.db.url=${postgreSQLContainer12.jdbcUrl}",
                "no.nav.db.username=${postgreSQLContainer12.username}",
                "no.nav.db.password=${postgreSQLContainer12.password}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    init {
        wireMockServer = initWireMock(rootDirectory = "src/test/resources")

        MockConfiguration.config(
            wireMockServer = wireMockServer,
            port = 8085,
            azureV2Url = lokaltKjørendeAzureV2OrNull()
        ).forEach { t, u ->
            System.setProperty(t, u)
        }
    }

    @PostConstruct
    fun setupRestServiceServers() {
    }

    @BeforeAll
    fun setup() {
        cleanUpDB()
    }

    @AfterAll
    fun opprydning() {
        wireMockServer.stop()
        cleanUpDB()
    }

    @Test
    fun contextLoads(): Unit = runBlocking {
        assertThat(webTestClient).isNotNull
        healthCheck()
    }

    protected fun cleanUpDB() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, SøknadRepository.SØKNAD_TABLE)
        JdbcTestUtils.deleteFromTables(jdbcTemplate, BunkeRepository.BUNKE_TABLE)
        JdbcTestUtils.deleteFromTables(jdbcTemplate, MappeRepository.MAPPE_TABLE)
        JdbcTestUtils.deleteFromTables(jdbcTemplate, PersonRepository.PERSON_TABLE)
        JdbcTestUtils.deleteFromTables(jdbcTemplate, AksjonspunktRepository.AKSJONSPUNKT_TABLE)
        JdbcTestUtils.deleteFromTables(jdbcTemplate, JournalpostRepository.JOURNALPOST_TABLE)
    }

    fun healthCheck() {
        webTestClient
            .get()
            .uri { it.path("/internal/actuator/info").build() }
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java).isEqualTo("{}")
    }
}
