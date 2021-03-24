package no.nav.k9punsj.audit

import java.util.stream.Collectors


/**
 * Data som utgjør et innslag i sporingsloggen i "Common Event Format (CEF)".
 */
class Auditdata(private val header: AuditdataHeader, private val fields: Set<CefField>) {

    /**
     * Loggstreng i "Commen Event Format (CEF)".
     */
    override fun toString(): String {
        return header.toString() + fields.stream()
            .map { f: CefField -> f.toString() }
            .sorted()
            .collect(Collectors.joining(FIELD_SEPARATOR))
    }

    companion object {
        private const val FIELD_SEPARATOR = " "
    }
}
