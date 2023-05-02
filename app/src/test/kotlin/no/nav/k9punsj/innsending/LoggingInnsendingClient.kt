package no.nav.k9punsj.innsending

import no.nav.k9punsj.IkkeTestProfil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@IkkeTestProfil
class LoggingInnsendingClient : InnsendingClient {
    override fun send(pair: Pair<String, String>) {
        val (key, value) = pair
        logger.info("Innsending. Key=[$key], Value=[$value]")
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(LoggingInnsendingClient::class.java)
    }
}
