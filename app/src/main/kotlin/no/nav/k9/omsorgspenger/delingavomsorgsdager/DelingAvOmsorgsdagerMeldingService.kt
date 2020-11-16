package no.nav.k9.omsorgspenger.delingavomsorgsdager

import no.nav.k9.akjonspunkter.Aksjonspunkt
import no.nav.k9.akjonspunkter.AksjonspunktStatus
import no.nav.k9.fordel.PunsjEventDto
import no.nav.k9.journalpost.JournalpostRepository
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.objectMapper
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class DelingAvOmsorgsdagerMeldingService @Autowired constructor(
    val hendelseProducer: HendelseProducer,
    val journalpostRepository: JournalpostRepository,
) {
    private companion object {
        const val rapidTopic = "k9-rapid-v2"
        const val topicK9Los = "privat-k9punsj-aksjonspunkthendelse-v1"
    }

    internal suspend fun send(melding: OverføreOmsorgsdagerBehov, dedupKey: String) {
        val (id, overføring) = Behovssekvens(
            id = dedupKey,
            correlationId = UUID.randomUUID().toString(),
            behov = arrayOf(melding)
        ).keyValue

        hendelseProducer.send(topicName = rapidTopic, key = id, data = overføring)

        for (jornalpostId in melding.journalpostIder) {
            val jpost = journalpostRepository.hent(jornalpostId)
            hendelseProducer.send(
                topicName = topicK9Los, data = objectMapper().writeValueAsString(
                    PunsjEventDto(
                        jpost.uuid.toString(),
                        jornalpostId,
                        jpost.aktørId,
                        LocalDateTime.now(),
                        mutableMapOf(Aksjonspunkt.PUNSJ.kode to AksjonspunktStatus.UTFØRT.kode)
                    )
                ), key = id
            )
        }
    }
}
