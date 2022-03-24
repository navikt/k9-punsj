package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.JsonB
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.domenetjenester.dto.JournalpostId
import java.time.LocalDateTime
import java.util.UUID

typealias BrevId = String
typealias Mottaker = String

const val DEFAULT_EIER = "PUNSJ"

data class BrevEntitet(
    val brevId : BrevId = UUID.randomUUID().toString(),
    val forJournalpostId: JournalpostId,
    val brevData: DokumentbestillingDto,
    val brevType: BrevType,
    val opprettet_av: String = DEFAULT_EIER,
    val opprettet_tid: LocalDateTime? = null,
) {
    internal companion object {
        internal fun DokumentbestillingDto.toJsonB() : String {
            val jsonB = kotlin.runCatching { objectMapper().convertValue<JsonB>(this) }.getOrElse { throw it }
            return kotlin.runCatching { objectMapper().writeValueAsString(jsonB) }.getOrElse { throw it }
        }
    }
}


