package no.nav.k9punsj.brev

import no.nav.k9punsj.domenetjenester.dto.JournalpostId

interface BrevService {

    suspend fun hentBrevSendtUtPåJournalpost(journalpostId: JournalpostId) : List<BrevEntitet>

    suspend fun bestillBrev(
        forJournalpostId: JournalpostId,
        brevData: DokumentbestillingDto,
        brevType: BrevType,
        saksbehandler: String
    ) : BrevEntitet
}
