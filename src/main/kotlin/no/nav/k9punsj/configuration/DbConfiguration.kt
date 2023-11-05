package no.nav.k9punsj.configuration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.k9punsj.StandardProfil
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@StandardProfil
class DbConfiguration {

    @Bean
    fun hikariConfig(env: Map<String, String> = System.getenv()) = createHikariConfig(env)
}

fun hikariConfig(hikariConfig: DbConfiguration): HikariDataSource {
    migrate(hikariConfig)
    return getDataSource(hikariConfig)
}

private fun createHikariConfig(
    env: Map<String, String>
) = HikariConfig().apply {
    jdbcUrl = String.format(
        "jdbc:postgresql://%s:%s/%s",
        env.hentRequiredEnv("DATABASE_HOST"),
        env.hentRequiredEnv("DATABASE_PORT"),
        env.hentRequiredEnv("DATABASE_DATABASE")
    )

    username = env.hentRequiredEnv("DATABASE_USERNAME")
    password = env.hentRequiredEnv("DATABASE_PASSWORD")
    maximumPoolSize = 3
    minimumIdle = 1
    idleTimeout = 10001
    connectionTimeout = 1000
    maxLifetime = 30001
    driverClassName = "org.postgresql.Driver"
}

private fun Map<String, String>.hentRequiredEnv(key: String) : String = requireNotNull(get(key)) {
    "Environment variable $key må være satt"
}

private fun getDataSource(configuration: DbConfiguration): HikariDataSource =
    HikariDataSource(configuration.hikariConfig())

private fun migrate(configuration: DbConfiguration) =
    runMigration(HikariDataSource(configuration.hikariConfig()))

private fun loadFlyway(dataSource: DataSource, initSql: String? = null) =
    Flyway.configure()
        .locations("migreringer/")
        .dataSource(dataSource)
        .initSql(initSql)
        .load()!!

private fun runMigration(dataSource: DataSource, initSql: String? = null): MigrateResult? {
    return loadFlyway(dataSource, initSql).migrate()
}