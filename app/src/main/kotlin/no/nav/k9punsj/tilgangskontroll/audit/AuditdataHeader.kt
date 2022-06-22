package no.nav.k9punsj.tilgangskontroll.audit

class AuditdataHeader(
    private val vendor: String,
    private val product: String,
    private val eventClassId: EventClassId,
    private val name: String,
    private val severity: String
) {

    /**
     * Loggheader i "Commen Event Format (CEF)".
     */
    override fun toString(): String {
        return String.format(
            "CEF:0|%s|%s|%s|%s|%s|%s|",
            cefHeaderEscape(vendor),
            cefHeaderEscape(product),
            cefHeaderEscape(logVersion),
            cefHeaderEscape(eventClassId.cefKode),
            cefHeaderEscape(name),
            cefHeaderEscape(severity)
        )
    }

    companion object {
        /**
         * Loggversjon som endres ved nye felter. Ved bytte avtales nytt format med Arcsight-gjengen.
         */
        private const val logVersion = "1.0"
        private fun cefHeaderEscape(s: String): String {
            return s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "").replace("\r", "")
        }
    }
}
