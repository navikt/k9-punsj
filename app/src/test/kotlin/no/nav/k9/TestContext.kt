package no.nav.k9

import no.nav.k9.kafka.HendelseProducer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

@TestConfiguration
@Profile("test")
class TestContext {

    @Bean
    fun hendelseProducerBean() = hendelseProducerMock

    val hendelseProducerMock: HendelseProducer = object: HendelseProducer {
        override fun send(topicName: String, søknadString: String, søknadId: String) {

        }
    }
}
