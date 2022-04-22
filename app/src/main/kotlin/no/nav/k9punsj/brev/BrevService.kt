package no.nav.k9punsj.brev

interface BrevService {

    suspend fun hentBrevSendtUtPåJournalpost(journalpostId: String) : List<BrevEntitet>

    suspend fun bestillBrev(
        forJournalpostId: String,
        brevData: DokumentbestillingDto,
        brevType: BrevType,
        saksbehandler: String
    ) : BrevEntitet
}
