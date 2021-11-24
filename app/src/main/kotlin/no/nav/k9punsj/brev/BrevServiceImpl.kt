package no.nav.k9punsj.brev

import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9punsj.domenetjenester.mappers.MapDokumentTilK9Formidling
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
    val pdlService: PdlService
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
        val (bestilling, feil) = MapDokumentTilK9Formidling(brevEntitet.brevId, brevEntitet.brevData, pdlService).bestillingOgFeil()

        if (feil.isEmpty()) {
            hendelseProducer.sendMedOnSuccess(SEND_BREVBESTILLING_TIL_K9_FORMIDLING,
                bestilling.toJson(),
                brevEntitet.brevId) {
                runBlocking { lagreUnnaBrevSomErUtsendt(brevEntitet, saksbehandler) }
            }
        } else {
            throw IllegalStateException("Klarte ikke bestille brev, feiler med $feil")
        }
        return brevEntitet
    }

    private suspend fun lagreUnnaBrevSomErUtsendt(brevEntitet: BrevEntitet, saksbehandler: String) {
        val brev = brevRepository.opprettBrev(brevEntitet, saksbehandler)
        log.info("""Punsj har sendt brevbestilling for journalpostId(${brev.forJournalpostId})""")

    }

    private fun Dokumentbestilling.toJson(): String {
        return kotlin.runCatching { objectMapper().writeValueAsString(this) }.getOrElse { throw it }
    }
}
