package no.nav.k9punsj.db.config

import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

enum class Role {
    Admin, User, ReadOnly;
    override fun toString() = name.toLowerCase()
}

private val logger: Logger = LoggerFactory.getLogger(DbConfiguration::class.java)
fun getDataSource(configuration: DbConfiguration): HikariDataSource =
        if (configuration.isVaultEnabled()) {
            dataSourceFromVault(configuration, Role.Admin)
        } else {
            HikariDataSource(configuration.hikariConfig())
        }

fun dataSourceFromVault(hikariConfig: DbConfiguration, role: Role): HikariDataSource =
        HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                hikariConfig.hikariConfig(),
                hikariConfig.getVaultDbPath(),
                "${hikariConfig.databaseName()}-$role"
        )

fun migrate(configuration: DbConfiguration) =
        if (configuration.isVaultEnabled()) {
            runMigration(
                    dataSourceFromVault(configuration, Role.Admin),
                    "SET ROLE \'${configuration.databaseName()}-${Role.Admin}\'"
            )
        } else {
            runMigration(HikariDataSource(configuration.hikariConfig()))
        }

fun runMigration(dataSource: DataSource, initSql: String? = null): MigrateResult? {

    val flyway = Flyway.configure()
        .locations("migreringer/")
        .dataSource(dataSource)
        .initSql(initSql)
        .load()

    try {
        flyway.migrate()
    } catch (fwe: FlywayException) {
        try {
            // prøver igjen
            flyway.clean()
            return flyway.migrate()
        } catch (fwe2: FlywayException) {
            throw IllegalStateException("Migrering feiler", fwe2)
        }
    }
    return null

}
