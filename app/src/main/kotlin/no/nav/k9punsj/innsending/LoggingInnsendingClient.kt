package no.nav.k9punsj.innsending

import org.slf4j.LoggerFactory

internal class LoggingInnsendingClient : InnsendingClient {
    override fun send(pair: Pair<String, String>) {
        logger.warn("Sender Key=[${pair.first}], Value=[${pair.second}]")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(LoggingInnsendingClient::class.java)
    }
}