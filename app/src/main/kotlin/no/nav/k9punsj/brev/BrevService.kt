package no.nav.k9punsj.brev

import no.nav.k9punsj.rest.web.JournalpostId

interface BrevService {

    suspend fun lagreUnnaBrevSomErUtsendt(brevEntitet: BrevEntitet)

    suspend fun hentBrevSendtUtPåJournalpost(journalpostId: JournalpostId) : List<BrevEntitet>

    suspend fun bestillBrev()
}
