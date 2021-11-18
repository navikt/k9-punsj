package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.JsonB
import no.nav.k9punsj.objectMapper

data class BrevData(
    val mottaker: String
)

internal fun BrevData.toJsonB() : String {
    val jsonB = kotlin.runCatching { objectMapper().convertValue<JsonB>(this) }.getOrElse { throw it }
    return kotlin.runCatching { objectMapper().writeValueAsString(jsonB) }.getOrElse { throw it }
}
