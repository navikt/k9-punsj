package no.nav.k9punsj

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.impl.annotations.MockK
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.K9PunsjApplicationWithMocks.Companion.startup
import no.nav.k9punsj.akjonspunkter.AksjonspunktRepository
import no.nav.k9punsj.domenetjenester.repository.BunkeRepository
import no.nav.k9punsj.domenetjenester.repository.MappeRepository
import no.nav.k9punsj.domenetjenester.repository.PersonRepository
import no.nav.k9punsj.domenetjenester.repository.SøknadRepository
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.wiremock.initWireMock
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.time.Duration
import javax.sql.DataSource
import kotlin.concurrent.thread


private class PostgreSQLContainer12 : PostgreSQLContainer<PostgreSQLContainer12>("postgres:12.2-alpine")
private class KafkaContainer741 : KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.1"))

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@EnableMockOAuth2Server
@SpringBootTest
abstract class AbstractContainerBaseTest {

    private lateinit var postgreSQLContainer12: PostgreSQLContainer12

    var port: Int = 0

    @Autowired
    lateinit var personRepository: PersonRepository

    @Autowired
    lateinit var mappeRepository: MappeRepository

    @Autowired
    lateinit var søknadRepository: SøknadRepository

    @Autowired
    lateinit var bunkeRepository: BunkeRepository

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    @Autowired
    lateinit var aksjonspunktRepository: AksjonspunktRepository

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Bean
    fun dataSource(): DataSource {
        HikariConfig().apply {
            jdbcUrl = postgreSQLContainer12.jdbcUrl
            username = postgreSQLContainer12.username
            password = postgreSQLContainer12.password
        }.also {
            return HikariDataSource(it)
        }
    }


    @Autowired
    lateinit var safGateway: SafGateway


    @PostConstruct
    fun setupRestServiceServers() {
    }

    companion object {

        init {
            val wireMockServer = initWireMock(
                port = 8084,
                rootDirectory = "src/test/resources"
            )

            startup(
                wireMockServer = wireMockServer,
                port = 8085,
                azureV2Url = lokaltKjørendeAzureV2OrNull(),
                profiles = "test"
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
            }.also {
                threads.add(it)
            }

            thread {
                KafkaContainer741().apply {
                    start()
                    System.setProperty("KAFKA_BROKERS", bootstrapServers)
                }
            }.also { threads.add(it) }

            threads.forEach { it.join() }
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

    @AfterAll
    fun opprydning() {
    }

    @BeforeAll
    fun setup() {
        //vedtakKafkaConsumer.subscribeHvisIkkeSubscribed(VEDTAK_TOPIC)
        port = postgreSQLContainer12.exposedPorts.first()
    }

}