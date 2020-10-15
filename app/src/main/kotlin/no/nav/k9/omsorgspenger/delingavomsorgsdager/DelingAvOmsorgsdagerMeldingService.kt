//package no.nav.k9.omsorgspenger.delingavomsorgsdager
//
//import no.nav.k9.kafka.HendelseProducer
//import no.nav.k9.rapid.behov.Behovssekvens
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.stereotype.Service
//import java.util.*
//
//@Service
//class DelingAvOmsorgsdagerMeldingService @Autowired constructor(
//        val hendelseProducer: HendelseProducer
//){
//    private companion object {
//        const val rapidTopic = "k9-rapid-v1"
//    }
//
//    internal suspend fun send(melding: DelingAvOmsorgsdagerBehov, dedupKey: String) {
//        val (id, overføring) = Behovssekvens(
//                id = dedupKey,
//                correlationId = UUID.randomUUID().toString(),
//                behov = arrayOf(melding)
//        ).keyValue
//
//        hendelseProducer.send(topicName = rapidTopic, key = id, data = overføring)
//    }
//}
