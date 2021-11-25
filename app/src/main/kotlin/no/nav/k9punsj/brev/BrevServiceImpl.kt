package no.nav.k9punsj.brev

import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9punsj.domenetjenester.mappers.MapDokumentTilK9Formidling
import no.nav.k9punsj.journalpost.JournalpostService
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics.Companion.SEND_BREVBESTILLING_TIL_K9_FORMIDLING
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import no.nav.k9punsj.rest.web.JournalpostId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrevServiceImpl(
    val brevRepository: BrevRepository,
    val hendelseProducer: HendelseProducer,
    val pdlService: PdlService,
    val journalpostService: JournalpostService
) : BrevService {

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(BrevServiceImpl::class.java)
    }

    override suspend fun hentBrevSendtUtPåJournalpost(journalpostId: JournalpostId): List<BrevEntitet> {
        return brevRepository.hentAlleBrevPåJournalpost(journalpostId)
    }

    override suspend fun bestillBrev(
        forJournalpostId: JournalpostId,
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
            val (bestilling, feil) = MapDokumentTilK9Formidling(brevEntitet.brevId,
                brevEntitet.brevData,
                pdlService).bestillingOgFeil()

            if (feil.isEmpty()) {
                val data = bestilling.toJson()
                hendelseProducer.sendMedOnSuccess(SEND_BREVBESTILLING_TIL_K9_FORMIDLING,
                    data,
                    brevEntitet.brevId) {
                    runBlocking { lagreUnnaBrevSomErUtsendt(brevEntitet, saksbehandler, data) }
                }
            } else {
                throw IllegalStateException("Klarte ikke bestille brev, feiler med $feil")
            }
            return brevEntitet
        } else {
            throw throw IllegalStateException("Kan ikke bestille brev på en journalpost som er ferdig behandlet av punsj")
        }
    }

    private suspend fun lagreUnnaBrevSomErUtsendt(brevEntitet: BrevEntitet, saksbehandler: String, data: String) {
        val brev = brevRepository.opprettBrev(brevEntitet, saksbehandler)
        log.info("""Punsj har sendt brevbestilling for journalpostId(${brev.forJournalpostId}) --> body er $data""")


    }

    private fun Dokumentbestilling.toJson(): String {
        return kotlin.runCatching { objectMapper().writeValueAsString(this) }.getOrElse { throw it }
    }
}
