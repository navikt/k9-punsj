package no.nav.k9punsj.innsending

import org.slf4j.LoggerFactory

import org.springframework.stereotype.Component

@Component
class LoggingInnsendingClient : InnsendingClient {
    override fun send(pair: Pair<String, String>) {
        logger.info("Innsending. Key=[${pair.first}], Value=[${pair.second}]")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(LoggingInnsendingClient::class.java)
    }
}