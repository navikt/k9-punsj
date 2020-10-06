package no.nav.k9.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
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
            val initSql = "SET ROLE \"${configuration.databaseName()}-${Role.Admin}\""
            logger.info("Initsql $initSql")
            runMigration(
                    dataSourceFromVault(configuration, Role.Admin),
                    initSql
            )
        } else {
            runMigration(HikariDataSource(configuration.hikariConfig()))
        }

fun runMigration(dataSource: DataSource, initSql: String? = null): Int {

    return Flyway.configure()
            .locations("migreringer/")
            .dataSource(dataSource)
            .initSql(initSql)
            .load()
            .migrate()
}
