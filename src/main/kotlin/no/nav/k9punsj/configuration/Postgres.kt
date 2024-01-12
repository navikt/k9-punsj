import com.zaxxer.hikari.HikariDataSource
import no.nav.k9punsj.configuration.DbConfiguration
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import java.util.*

enum class Role {
    Admin;

    override fun toString() = name.lowercase(Locale.getDefault())
}

fun getDataSource(dbConfiguration: DbConfiguration): HikariDataSource =
    if (dbConfiguration.isVaultEnabled()) {
        dataSourceFromVault(dbConfiguration, Role.Admin)
    } else {
        HikariDataSource(dbConfiguration.hikariConfig())
    }

fun dataSourceFromVault(dbConfiguration: DbConfiguration, role: Role): HikariDataSource =
    HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
        dbConfiguration.hikariConfig(),
        dbConfiguration.getVaultDbPath(),
        "${dbConfiguration.databaseName()}-$role"
    )
