package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.convertValue
import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9punsj.felles.JsonB
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics.Companion.SEND_BREVBESTILLING_TIL_K9_FORMIDLING
import no.nav.k9punsj.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrevServiceImpl(
    val brevRepository: BrevRepository,
    val hendelseProducer: HendelseProducer,
    val personService: PersonService,
    val journalpostService: JournalpostService
) : BrevService {

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(BrevServiceImpl::class.java)
    }

    override suspend fun hentBrevSendtUtPåJournalpost(journalpostId: String): List<BrevEntitet> {
        return brevRepository.hentAlleBrevPåJournalpost(journalpostId)
    }

    override suspend fun bestillBrev(
        forJournalpostId: String,
        brevData: DokumentbestillingDto,
        brevType: BrevType,
        saksbehandler: String
    ) : BrevEntitet {
        val brevEntitet = BrevEntitet(
            forJournalpostId = forJournalpostId,
            brevType = brevType,
            brevData = brevData
        )

        val kanSendeInn = journalpostService.kanSendeInn(brevEntitet.forJournalpostId)
        if (kanSendeInn) {
            val aktørId = personService.finnPersonVedNorskIdentFørstDbSåPdl(brevData.soekerId).aktørId
            val (bestilling, feil) = MapDokumentTilK9Formidling(brevEntitet.brevId,
                brevEntitet.brevData,
                aktørId).bestillingOgFeil()

            if (feil.isEmpty()) {
                val data = kotlin.runCatching { bestilling.toJsonB() }.getOrElse { throw it }

                hendelseProducer.sendMedOnSuccess(SEND_BREVBESTILLING_TIL_K9_FORMIDLING,
                    data,
                    brevEntitet.brevId) {
                    runBlocking { lagreUnnaBrevSomErUtsendt(brevEntitet, saksbehandler) }
                }
            } else {
                throw IllegalStateException("Klarte ikke bestille brev, feiler med $feil")
            }
            return brevEntitet
        } else {
            throw throw IllegalStateException("Kan ikke bestille brev på en journalpost som er ferdig behandlet av punsj")
        }
    }

    private suspend fun lagreUnnaBrevSomErUtsendt(brevEntitet: BrevEntitet, saksbehandler: String) {
        val brev = brevRepository.opprettBrev(brevEntitet, saksbehandler)
        log.info("""Punsj har sendt brevbestilling for journalpostId(${brev.forJournalpostId})""")
    }

    private fun Dokumentbestilling.toJsonB() : String {
        val jsonB = kotlin.runCatching { objectMapper().convertValue<JsonB>(this) }.getOrElse { throw it }
        return kotlin.runCatching { objectMapper().writeValueAsString(jsonB) }.getOrElse { throw it }
    }
}
