package no.nav.k9.fordel

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.io.IOException


@Service
class Consumer {
    private val logger: Logger = LoggerFactory.getLogger(Consumer::class.java)

    @KafkaListener(topics = ["privat-k9-fordel-journalforing-v1"], groupId = "k9-punsj")
    @Throws(IOException::class)
    fun consume(message: String?) {
        logger.info(String.format("#### -> Consumed message -> %s", message))
        
    }
}