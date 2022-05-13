package no.nav.k9punsj.brev

import java.time.LocalDateTime
import java.util.UUID

const val DEFAULT_EIER = "PUNSJ"

data class BrevEntitet(
    val brevId : String = UUID.randomUUID().toString(),
    val forJournalpostId: String,
    val brevData: DokumentbestillingDto,
    val brevType: BrevType,
    val opprettet_av: String = DEFAULT_EIER,
    val opprettet_tid: LocalDateTime? = null,
)


