package no.nav.k9punsj.util

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

class DbContainerInitializer: ApplicationContextInitializer<ConfigurableApplicationContext> {
    companion object {
        private val postgreSQLContainer12 = PostgreSQLContainer("postgres:12.2-alpine")

        internal val postgresContainer = postgreSQLContainer12
            .withDatabaseName("postgres")
            .withUsername("postgres")
            .withPassword("postgres")
        }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        postgresContainer.start()
    }
}