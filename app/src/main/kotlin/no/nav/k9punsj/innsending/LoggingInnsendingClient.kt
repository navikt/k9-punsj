package no.nav.k9punsj.innsending

import org.slf4j.LoggerFactory

class LoggingInnsendingClient : InnsendingClient {
    override fun send(pair: Pair<String, String>) {
        logger.error("Innsending feilet. Key=[${pair.first}], Value=[${pair.second}]")
    }

    override fun toString() = "LoggingInnsendingClient: Innsendinger vil kun bli logget, ikke sendt videre."

    private companion object {
        private val logger = LoggerFactory.getLogger(LoggingInnsendingClient::class.java)
    }
}