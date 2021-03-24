package no.nav.k9punsj.audit

import no.nav.k9punsj.AppConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class Auditlogger constructor(
    val configuration: AppConfiguration,
    val isEnabled: Boolean = configuration.auditEnabled(),
    val defaultVendor: String = configuration.auditVendor(),
    val defaultProduct: String = configuration.auditProduct()
) {

    fun logg(auditdata: Auditdata) {
        if (isEnabled) {
            auditLogger.info(auditdata.toString())
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Auditlogger::class.java)
        private val auditLogger: Logger = LoggerFactory.getLogger("auditLogger")
    }
}
