package no.nav.k9punsj

import no.nav.k9punsj.configuration.DbConfiguration
import no.nav.k9punsj.util.DatabaseUtil
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

    private fun hikariConfigLocal(hikariConfig: DbConfiguration): DataSource {
        val dataSource = DatabaseUtil.dataSource
        runMigrationLocal(dataSource)
        return dataSource
    }

    private fun runMigrationLocal(dataSource: DataSource): MigrateResult? {
        val load = Flyway.configure()
            .cleanDisabled(false)
            .locations("migreringer/")
            .dataSource(dataSource)
            .load()
        return try {
            load.migrate()
        } catch (fwe: FlywayException) {
            // prøver igjen siden kjører lokalt
            load.clean()
            try {
                load.migrate()
            } catch (fwe: FlywayException) {
                throw IllegalStateException("Migrering feiler", fwe)
            }
        }
    }
}
