package no.nav.k9punsj.omsorgspenger.overfoerdager

import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.OverføreOmsorgsdagerBehov
import no.nav.k9punsj.akjonspunkter.AksjonspunktKode
import no.nav.k9punsj.akjonspunkter.AksjonspunktStatus
import no.nav.k9punsj.fordel.PunsjEventDto
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class OverførDagerSøknadService @Autowired constructor(
        val hendelseProducer: HendelseProducer,
        val journalpostRepository: JournalpostRepository,
){
    private companion object {
        const val rapidTopic = "k9-rapid-v2"

        private val log: Logger = LoggerFactory.getLogger(OverførDagerSøknadService::class.java)
    }

    //TODO(OJR) lagre i databasen? + oppdatering av journalpost? + abac
    internal suspend fun sendSøknad(søknad: OverføreOmsorgsdagerBehov, dedupKey: String) {
        val (id, overføring) = Behovssekvens(
                id = dedupKey,
                correlationId = UUID.randomUUID().toString(),
                behov = arrayOf(søknad)
        ).keyValue

        hendelseProducer.send(topicName = rapidTopic, key = id, data = overføring)

        for (jornalpostId in søknad.journalpostIder) {
            val jpost = journalpostRepository.hent(jornalpostId)
            val data = objectMapper().writeValueAsString(PunsjEventDto(
                    jpost.uuid.toString(),
                    jornalpostId,
                    jpost.aktørId,
                    LocalDateTime.now(),
                    aksjonspunktKoderMedStatusListe = mutableMapOf(AksjonspunktKode.PUNSJ.kode to AksjonspunktStatus.UTFØRT.kode)
            ))
            log.info(data)
            hendelseProducer.send(topicName = Topics.SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS, data = data, key = id)
        }
    }
}
