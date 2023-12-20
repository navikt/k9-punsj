package no.nav.k9punsj

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import com.ninjasquad.springmockk.MockkBean
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.k9punsj.tilgangskontroll.abac.IPepClient
import no.nav.k9punsj.wiremock.initWireMock
import no.nav.k9punsj.wiremock.saksbehandlerAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI
import kotlin.concurrent.thread


private class PostgreSQLContainer12 : PostgreSQLContainer<PostgreSQLContainer12>("postgres:12.2-alpine")
private class KafkaContainer741 : KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.1"))

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    classes = [K9PunsjApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@ExtendWith(SpringExtension::class)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
abstract class AbstractContainerBaseTest {

    private lateinit var postgreSQLContainer12: PostgreSQLContainer12

    @MockkBean
    lateinit var pepClient: IPepClient

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @PostConstruct
    fun setupRestServiceServers() {
    }

    @BeforeAll
    fun setup() {
    }

    @AfterAll
    fun opprydning() {
        wireMockServer.stop()
    }

    @Test
    fun contextLoads(): Unit = runBlocking {
        assertThat(webTestClient).isNotNull
        healthCheck()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractContainerBaseTest::class.java)
        val saksbehandlerAuthorizationHeader = "Bearer ${Azure.V2_0.saksbehandlerAccessToken()}"

        val wireMockServer: WireMockServer

        init {
            wireMockServer = initWireMock(
                port = 8084,
                rootDirectory = "src/test/resources"
            )


            val threads = mutableListOf<Thread>()
            thread {
                PostgreSQLContainer12().apply {
                    // Cloud SQL har wal_level = 'logical' på grunn av flagget cloudsql.logical_decoding i
                    // naiserator.yaml. Vi må sette det samme lokalt for at flyway migrering skal fungere.
                    withCommand("postgres", "-c", "wal_level=logical")
                    start()
                    System.setProperty("spring.datasource.url", jdbcUrl)
                    System.setProperty("spring.datasource.username", username)
                    System.setProperty("spring.datasource.password", password)
                }
            }.also { threads.add(it) }

            thread {
                KafkaContainer741().apply {
                    start()
                    System.setProperty("KAFKA_BROKERS", bootstrapServers)
                }
            }.also { threads.add(it) }

            threads.forEach { it.join() }

            MockConfiguration.config(
                wireMockServer = wireMockServer,
                port = 8085,
                azureV2Url = lokaltKjørendeAzureV2OrNull()
            ).forEach { t, u ->
                System.setProperty(t, u)
            }
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

    fun healthCheck() {
        webTestClient
            .get()
            .uri { it.path("/internal/actuator/info").build() }
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java).isEqualTo("{}")
    }
}
