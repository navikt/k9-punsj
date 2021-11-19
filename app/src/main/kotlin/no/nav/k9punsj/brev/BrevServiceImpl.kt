package no.nav.k9punsj.brev

import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.kafka.Topics
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.JournalpostId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrevServiceImpl(
    val brevRepository: BrevRepository,
    val hendelseProducer: HendelseProducer,
) : BrevService {

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(BrevServiceImpl::class.java)
    }


    override suspend fun lagreUnnaBrevSomErUtsendt(brevEntitet: BrevEntitet) {
        brevRepository.opprettBrev(brevEntitet)
    }

    override suspend fun hentBrevSendtUtPåJournalpost(journalpostId: JournalpostId): List<BrevEntitet> {
        return brevRepository.hentAlleBrevPåJournalpost(journalpostId)
    }

    override suspend fun bestillBrev() {
        val nyId = BrevId().nyId()

        val data = "json"
        hendelseProducer.sendMedOnSuccess(Topics.SEND_BREVBESTILLING_TIL_K9_FORMIDLING, data,  nyId) {}
    }


    fun lagDokumentbestillingPåJournalpost(journalpostId: JournalpostId, saksnummer: String?, aktørId: AktørId): String {
        val dokumentbestilling = Dokumentbestilling()
        dokumentbestilling.eksternReferanse = journalpostId
        dokumentbestilling.saksnummer = saksnummer ?: "GSAK"
        dokumentbestilling.aktørId = aktørId

        // utled?
        dokumentbestilling.dokumentMal = DokumentMalType.INNTEKTSMELDING_FOR_TIDLIG_DOK.kode


        return kotlin.runCatching { objectMapper().writeValueAsString(dokumentbestilling) }.getOrElse { throw it }
    }

}
