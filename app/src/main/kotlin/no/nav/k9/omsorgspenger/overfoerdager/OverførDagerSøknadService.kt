package no.nav.k9.omsorgspenger.overfoerdager

import no.nav.k9.akjonspunkter.Aksjonspunkt
import no.nav.k9.fordel.PunsjEventDto
import no.nav.k9.journalpost.JournalpostRepository
import no.nav.k9.kafka.HendelseProducer
import no.nav.k9.objectMapper
import no.nav.k9.omsorgspenger.delingavomsorgsdager.DelingAvOmsorgsdagerMeldingService
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class OverførDagerSøknadService @Autowired constructor(
        val hendelseProducer: HendelseProducer,
        val journalpostRepository: JournalpostRepository,
){
    private companion object {
        const val rapidTopic = "k9-rapid-v2"
        const val topicK9Los = "privat-k9punsj-aksjonspunkthendelse-v1"
    }

    internal suspend fun sendSøknad(søknad: OverføreOmsorgsdagerBehov, dedupKey: String) {
        val (id, overføring) = Behovssekvens(
                id = dedupKey,
                correlationId = UUID.randomUUID().toString(),
                behov = arrayOf(søknad)
        ).keyValue

        hendelseProducer.send(topicName = rapidTopic, key = id, data = overføring)

        for (jornalpostId in søknad.journalpostIder) {
            val jpost = journalpostRepository.hent(jornalpostId)
            hendelseProducer.send(topicName = OverførDagerSøknadService.topicK9Los, data = objectMapper().writeValueAsString(PunsjEventDto(
                    jpost.uuid.toString(),
                    jornalpostId,
                    jpost.aktørId,
                    LocalDateTime.now(),
                    mutableListOf(Aksjonspunkt.AVSLUTTET))), key = id)
        }
    }
}
