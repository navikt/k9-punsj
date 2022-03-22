package no.nav.k9punsj.brev

import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.JsonB
import no.nav.k9punsj.domenetjenester.dto.JournalpostIdDto
import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto

data class DokumentbestillingDto(
    val journalpostId: JournalpostIdDto,
    val brevId: BrevId? = null,
    val saksnummer: String = GENERELL_SAK,
    val soekerId: NorskIdentDto,
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
