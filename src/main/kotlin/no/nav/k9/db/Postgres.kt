package no.nav.k9.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import javax.sql.DataSource

enum class Role {
    Admin, User, ReadOnly;
    override fun toString() = name.toLowerCase()
}

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
                    "SET ROLE \"${configuration.databaseName()}-${Role.Admin}\""
            )
        } else {
            runMigration(HikariDataSource(configuration.hikariConfig()))
        }

fun runMigration(dataSource: DataSource, initSql: String? = null): Int {

    // Todo: Fjernes når vi går i prod
    Flyway.configure()
            .locations("migreringer/")
            .dataSource(dataSource)
            .initSql(initSql)
            .load()
            .clean()

    return Flyway.configure()
            .locations("migreringer/")
            .dataSource(dataSource)
            .initSql(initSql)
            .load()
            .migrate()
}
