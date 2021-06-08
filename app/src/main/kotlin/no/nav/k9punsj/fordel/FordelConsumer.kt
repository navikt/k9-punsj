package no.nav.k9punsj.fordel

import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.kafka.KafkaConfig.Companion.ON_PREM_CONTAINER_FACTORY
import no.nav.k9punsj.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.io.IOException

@Service
@Profile("!test")
class FordelConsumer @Autowired constructor(val hendelseMottaker: HendelseMottaker) {
    private val logger: Logger = LoggerFactory.getLogger(FordelConsumer::class.java)

    @KafkaListener(
        topics = ["privat-k9-fordel-journalforing-v1"],
        groupId = "k9-punsj-3",
        properties = ["auto.offset.reset:earliest"],
        containerFactory = ON_PREM_CONTAINER_FACTORY
    )
    @Throws(IOException::class)
    fun consume(message: String?) {
        logger.info(String.format("#### -> Consumed message -> %s", message))
        val fordelPunsjEventDto = objectMapper().readValue(message, FordelPunsjEventDto::class.java)
        runBlocking {
            hendelseMottaker.prosesser(fordelPunsjEventDto)
        }
    }
}
