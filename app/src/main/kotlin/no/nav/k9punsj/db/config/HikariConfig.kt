package no.nav.k9punsj.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.k9punsj.db.config.DbConfiguration
import no.nav.k9punsj.db.config.getDataSource
import no.nav.k9punsj.db.config.migrate

fun createHikariConfig(
        jdbcUrl: String,
        username: String? = null,
        password: String? = null
) = HikariConfig().apply {
    this.jdbcUrl = jdbcUrl
    maximumPoolSize = 10
    minimumIdle = 1
    idleTimeout = 10001
    connectionTimeout = 1000
    maxLifetime = 30001
    driverClassName = "org.postgresql.Driver"
    username?.let { this.username = it }
    password?.let { this.password = it }
}

fun hikariConfig(hikariConfig: DbConfiguration): HikariDataSource {
    migrate(hikariConfig)
    return getDataSource(hikariConfig)
}
