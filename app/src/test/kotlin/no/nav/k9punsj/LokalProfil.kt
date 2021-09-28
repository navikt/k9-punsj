package no.nav.k9punsj

import com.zaxxer.hikari.HikariDataSource
import no.nav.k9punsj.db.config.DbConfiguration
import no.nav.k9punsj.db.config.getDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.output.MigrateResult
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

@Profile("local")
annotation class LokalProfil

@TestConfiguration
@LokalProfil
class LokalBeans {
    @Bean
    fun lokalDataSource(dbConfiguration: DbConfiguration): DataSource {
        return hikariConfigLocal(dbConfiguration)
    }

    private fun hikariConfigLocal(hikariConfig: DbConfiguration): HikariDataSource {
        runMigrationLocal(hikariConfig)
        return getDataSource(hikariConfig)
    }

    private fun runMigrationLocal(configuration: DbConfiguration): MigrateResult? {
        val hikariDataSource = HikariDataSource(configuration.hikariConfig())
        val load = Flyway.configure()
            .locations("migreringer/")
            .dataSource(hikariDataSource)
            .load()
        return try {
            load.migrate()
        } catch (fwe: FlywayException) {
            //prøver igjen siden kjører lokalt
            load.clean()
            try {
                load.migrate()
            } catch (fwe: FlywayException) {
                throw IllegalStateException("Migrering feiler", fwe)
            }
        }
    }

}