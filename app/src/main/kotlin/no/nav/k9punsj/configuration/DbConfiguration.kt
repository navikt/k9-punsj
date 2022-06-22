package no.nav.k9punsj.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DbConfiguration(
    @Value("\${no.nav.db.url}") private val url: String,
    @Value("\${no.nav.db.username}") private val username: String,
    @Value("\${no.nav.db.password}") private val password: String?,
    @Value("\${no.nav.db.vault_mountpath}") private val vaultMountpath: String
) {

    @Bean
    fun hikariConfig() = createHikariConfig(url, username, password)

    @Bean
    fun isVaultEnabled(): Boolean {
        return vaultMountpath.isNotBlank()
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
