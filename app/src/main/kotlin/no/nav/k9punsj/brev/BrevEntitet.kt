package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.felles.JsonB
import no.nav.k9punsj.objectMapper
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
) {
    internal companion object {
        internal fun DokumentbestillingDto.toJsonB() : String {
            val jsonB = kotlin.runCatching { objectMapper().convertValue<JsonB>(this) }.getOrElse { throw it }
            return kotlin.runCatching { objectMapper().writeValueAsString(jsonB) }.getOrElse { throw it }
        }
    }
}


