package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9punsj.brev.dto.BrevType
import no.nav.k9punsj.brev.dto.DokumentbestillingDto
import no.nav.k9punsj.felles.JsonB
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
internal class BrevServiceImpl(
    val hendelseProducer: HendelseProducer,
    val personService: PersonService,
    val journalpostService: JournalpostService,
    @Value("\${no.nav.kafka.k9_formidling.topic}") private val brevBestillingTopic: String
) : BrevService {

    override suspend fun bestillBrev(
        dokumentbestillingDto: DokumentbestillingDto,
        brevType: BrevType,
        saksbehandler: String
    ): Boolean {
        val aktørId = personService.finnPersonVedNorskIdentFørstDbSåPdl(dokumentbestillingDto.soekerId).aktørId

        val (bestilling, feil) = MapDokumentTilK9Formidling(
            dto = dokumentbestillingDto,
            aktørId = aktørId
        ).bestillingOgFeil()

        if (feil.isEmpty()) {
            val brevDataJson = kotlin.runCatching { bestilling.toJsonB() }.getOrElse { throw it }

            hendelseProducer.send(
                topicName = brevBestillingTopic,
                data = brevDataJson,
                key = dokumentbestillingDto.brevId?: UUID.randomUUID().toString()
            )
        } else {
            throw IllegalStateException("Klarte ikke bestille brev, feiler med $feil")
        }

        return true
    }

    private fun Dokumentbestilling.toJsonB(): String {
        val jsonB = kotlin.runCatching { objectMapper().convertValue<JsonB>(this) }.getOrElse { throw it }
        return kotlin.runCatching { objectMapper().writeValueAsString(jsonB) }.getOrElse { throw it }
    }
}
