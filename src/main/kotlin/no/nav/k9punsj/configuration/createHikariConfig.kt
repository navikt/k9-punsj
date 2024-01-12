package no.nav.k9punsj.configuration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import getDataSource

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

fun createHikariDatasource(dbConfiguration: DbConfiguration): HikariDataSource {
    return getDataSource(dbConfiguration)
}
