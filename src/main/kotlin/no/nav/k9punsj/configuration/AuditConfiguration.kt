package no.nav.k9punsj.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuditConfiguration(
    @Value("\${no.nav.audit.enabled}") private val isAuditEnabled: Boolean,
    @Value("\${no.nav.audit.vendor}") private val auditVendor: String,
    @Value("\${no.nav.audit.product}") private val auditProduct: String,
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

}
