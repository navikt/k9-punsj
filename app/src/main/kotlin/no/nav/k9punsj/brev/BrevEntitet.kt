package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.JsonB
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.JournalpostId
import java.util.UUID

typealias BrevId = String
typealias Mottaker = String

data class BrevEntitet(
    val brevId : BrevId,
    val forJournalpostId: JournalpostId,
    val brevData: DokumentbestillingDto,
    val brevType: BrevType
)

internal fun BrevId.nyId() : BrevId {
    return UUID.randomUUID().toString()
}

internal fun DokumentbestillingDto.toJsonB() : String {
    val jsonB = kotlin.runCatching { objectMapper().convertValue<JsonB>(this) }.getOrElse { throw it }
    return kotlin.runCatching { objectMapper().writeValueAsString(jsonB) }.getOrElse { throw it }
}
