package no.nav.k9punsj.db.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

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
    isAutoCommit = true
}

fun hikariConfig(hikariConfig: DbConfiguration): HikariDataSource {
    migrate(hikariConfig)
    return getDataSource(hikariConfig)
}