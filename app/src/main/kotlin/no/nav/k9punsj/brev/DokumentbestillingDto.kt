package no.nav.k9punsj.brev

import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.db.datamodell.JsonB

data class DokumentbestillingDto(
    // lage egen ide for brev? eller bruke iden til journalposten?
    val eksternReferanse : String,

    // hva brukes denne til?
    val dokumentbestillingId: String,

    val saksnummer: String? = null,
    val aktørId: AktørId,
    val mottaker: Mottaker,
    val fagsakYtelseType: FagsakYtelseType,
    val dokumentMal: String,
    val avsenderApplikasjon: String,
    val dokumentdata: JsonB? = null

) {
    data class Mottaker(
        val type : String,
        val id : String
    )
}
