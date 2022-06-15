package no.nav.k9punsj.brev.dto

import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.felles.JsonB
import java.util.*

data class DokumentbestillingDto(
    val journalpostId: String?,
    val brevId: String = UUID.randomUUID().toString(),
    val saksnummer: String,
    val soekerId: String,
    val mottakerDto: MottakerDto,
    val fagsakYtelseType: FagsakYtelseType,
    val brevTittel: String,
    val dokumentMal: String,
    val dokumentdata: JsonB? = null,
    ) {

    internal companion object {
        const val GENERELL_SAK = "GENERELL_SAK"
    }
}