package no.nav.k9.omsorgspenger.overfoerdager

import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class OverførDagerSøknadService @Autowired constructor(
        val hendelseProducer: HendelseProducer
){
    private companion object {
        const val rapidTopic = "k9-rapid-v1"
    }

    internal suspend fun sendSøknad(søknad: OverføreOmsorgsdagerBehov, dedupKey: String) {
        val (id, overføring) = Behovssekvens(
                id = dedupKey,
                correlationId = UUID.randomUUID().toString(),
                behov = arrayOf(søknad)
        ).keyValue

        hendelseProducer.send(topicName = rapidTopic, key = id, data = overføring)
    }
}
