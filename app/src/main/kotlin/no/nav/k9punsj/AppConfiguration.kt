package no.nav.k9punsj

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class AppConfiguration(
    @Value("\${no.nav.audit.enabled}") private val isAuditEnabled: Boolean,
    @Value("\${no.nav.audit.vendor}") private val auditVendor: String,
    @Value("\${no.nav.audit.product}") private val auditProduct: String,
    @Value("\${no.nav.abac.url}") private val abacEndpointUrl: String,
    @Value("\${no.nav.abac.system_user}") private val abacUsername: String,
    @Value("\${no.nav.abac.system_user_password}") private val abacPassword: String,
) {

    @Bean(name = ["isAuditEnabled"])
    fun auditEnabled(): Boolean {
        return isAuditEnabled
    }

    @Bean
    fun auditVendor(): String {
        return auditVendor
    }

    @Bean
    fun auditProduct(): String {
        return auditProduct
    }

    @Bean
    fun abacEndpointUrl(): String {
        return abacEndpointUrl
    }

    @Bean
    fun abacUsername(): String {
        return abacUsername
    }

    @Bean
    fun abacPassword(): String {
        return abacPassword
    }
}

