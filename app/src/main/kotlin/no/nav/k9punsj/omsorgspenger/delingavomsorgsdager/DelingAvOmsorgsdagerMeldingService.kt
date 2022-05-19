package no.nav.k9punsj.omsorgspenger.delingavomsorgsdager

import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.fordel.PunsjEventDto
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class DelingAvOmsorgsdagerMeldingService @Autowired constructor(
    private val hendelseProducer: HendelseProducer,
    private val journalpostService: JournalpostService
) {
    private companion object {
        const val rapidTopic = "k9-rapid-v2"
    }

    internal suspend fun send(melding: OverføreOmsorgsdagerBehov, dedupKey: String) {
        val (id, overføring) = Behovssekvens(
            id = dedupKey,
            correlationId = UUID.randomUUID().toString(),
            behov = arrayOf(melding)
        ).keyValue

        hendelseProducer.send(topicName = rapidTopic, key = id, data = overføring)

        for (jornalpostId in melding.journalpostIder) {
            val jpost = journalpostService.hent(jornalpostId)

            kotlin.runCatching { objectMapper().writeValueAsString(
                PunsjEventDto(
                    jpost.uuid.toString(),
                    jornalpostId,
                    jpost.aktørId,
                    LocalDateTime.now(),
                    aksjonspunktKoderMedStatusListe = mutableMapOf(AksjonspunktKode.PUNSJ.kode to AksjonspunktStatus.UTFØRT.kode)
                )
            ) }.onSuccess {
                hendelseProducer.send(
                topicName = Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS,
                data = it,
                key = id
                )
            }.onFailure {
                throw IllegalArgumentException("Uventet mappingfeil", it)
            }
        }
    }
}
