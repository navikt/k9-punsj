package no.nav.k9punsj.brev

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.felles.JsonB
import no.nav.k9punsj.objectMapper

data class DokumentbestillingDto(
    val journalpostId: String,
    val brevId: String? = null,
    val saksnummer: String = GENERELL_SAK,
    val soekerId: String,
    val mottaker: Mottaker,
    val fagsakYtelseType: FagsakYtelseType,
    val dokumentMal: String,
    val dokumentdata: JsonB? = null,

    ) {
    data class Mottaker(
        val type: String,
        val id: String,
    )

    internal companion object {
        const val GENERELL_SAK = "GENERELL_SAK"

        internal fun DokumentbestillingDto.toJsonB() : String {
            val jsonB = kotlin.runCatching { objectMapper().convertValue<JsonB>(this) }.getOrElse { throw it }
            return kotlin.runCatching { objectMapper().writeValueAsString(jsonB) }.getOrElse { throw it }
        }
    }
}