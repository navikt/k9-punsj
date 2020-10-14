package no.nav.k9

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.k9.db.runMigration
import no.nav.k9.kafka.HendelseProducer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

@TestConfiguration
@Profile("test")
class TestContext {
    
    @Bean
    fun hendelseProducerBean() = hendelseProducerMock
    val hendelseProducerMock: HendelseProducer = object: HendelseProducer {
        override fun send(topicName: String, søknadString: String, søknadId: String) {

        }
    }

    @Bean
    fun databaseInitializer(): DataSource {
        val pg = EmbeddedPostgres.builder().setPort(5432).start()
        val dataSource = pg.postgresDatabase
        runMigration(dataSource)
        return dataSource
    }
}
