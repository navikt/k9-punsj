package no.nav.k9.db

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DbConfiguration(
        @Value("\${no.nav.db.url}") private val url: String,
        @Value("\${no.nav.db.username}") private val username: String,
        @Value("\${no.nav.db.password:#{null}}") private val password: String?,
        @Value("\${no.nav.db.vault_mountpath}") private val vaultMountpath: String
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(DbConfiguration::class.java)
    }

    @Bean
    fun hikariConfig() = createHikariConfig(url, username, password)

    @Bean
    fun isVaultEnabled(): Boolean {
        return !vaultMountpath.isBlank()
    }

    @Bean
    fun getVaultDbPath(): String {
        return vaultMountpath
    }

    @Bean
    fun databaseName(): String {
        return getDbNameFromUrl(url)
    }
}

fun getDbNameFromUrl(urlWithDbName: String): String {
    return Regex("(?<=/)[a-zA-Z][-_a-zA-Z\\d]*\$").find(urlWithDbName)?.value ?: ""
}