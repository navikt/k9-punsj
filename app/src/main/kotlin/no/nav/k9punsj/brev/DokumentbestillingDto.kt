package no.nav.k9punsj.brev

import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.JsonB

data class DokumentbestillingDto(
    val journalpostId: String,
    val brevId: BrevId? = null,
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

    companion object {
        const val GENERELL_SAK = "GENERELL_SAK"
    }
}
