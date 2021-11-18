package no.nav.k9punsj.brev

import no.nav.k9punsj.rest.web.JournalpostId
import java.util.UUID

typealias BrevId = String
typealias Mottaker = String

data class BrevEntitet(
    val brevId : BrevId,
    val forJournalpostId: JournalpostId,
    val brevData: BrevData,
    val brevType: BrevType
)

internal fun BrevId.nyId() : BrevId {
    return UUID.randomUUID().toString()
}
