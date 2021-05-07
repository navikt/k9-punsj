package no.nav.k9punsj.db.config

import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
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

fun runMigrationLocal(configuration: DbConfiguration, environment: Environment): MigrateResult? {
    val kanKjøreCleanEtterMigrate = environment.activeProfiles.size == 1 && environment.activeProfiles.contains("local")
    if(kanKjøreCleanEtterMigrate) {
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
    throw IllegalStateException("HEI!!!! Du prøver å kjøre flyway(migrate->clean->migrate) som sletter innholdet i databasen hvis migrering feiler! Må bare kjøres lokalt")
}

fun loadFlyway(dataSource: DataSource, initSql: String? = null) =
    Flyway.configure()
        .locations("migreringer/")
        .dataSource(dataSource)
        .initSql(initSql)
        .load()

fun runMigration(dataSource: DataSource, initSql: String? = null): MigrateResult? {
    return loadFlyway(dataSource, initSql).migrate()
}
