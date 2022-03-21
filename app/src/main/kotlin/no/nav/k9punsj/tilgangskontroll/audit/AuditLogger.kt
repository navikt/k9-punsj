package no.nav.k9punsj.tilgangskontroll.audit

import no.nav.k9punsj.AppConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class Auditlogger constructor(
    val configuration: AppConfiguration,
    @Qualifier("isAuditEnabled") val isEnabled: Boolean = configuration.auditEnabled(),
    @Qualifier("auditVendor") val defaultVendor: String = configuration.auditVendor(),
    @Qualifier("auditProduct") val defaultProduct: String = configuration.auditProduct()
) {

    fun logg(auditdata: Auditdata) {
        if (isEnabled) {
            auditLogger.info(auditdata.toString())
        }
    }

    companion object {
        private val auditLogger: Logger = LoggerFactory.getLogger("auditLogger")
    }
}
