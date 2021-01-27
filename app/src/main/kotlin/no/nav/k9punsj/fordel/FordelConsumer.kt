package no.nav.k9punsj.fordel

import no.nav.k9punsj.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.io.IOException


@Service
class FordelConsumer @Autowired constructor(val hendelseMottaker: HendelseMottaker) {
    private val logger: Logger = LoggerFactory.getLogger(FordelConsumer::class.java)

    @KafkaListener(topics = ["privat-k9-fordel-journalforing-v1"], groupId = "k9-punsj")
    @Throws(IOException::class)
    suspend fun consume(message: String?) {
        logger.info(String.format("#### -> Consumed message -> %s", message))
        val fordelPunsjEventDto = objectMapper().readValue(message, FordelPunsjEventDto::class.java)
        hendelseMottaker.prosesser(fordelPunsjEventDto.journalpostId, fordelPunsjEventDto.aktørId)
    }
}
