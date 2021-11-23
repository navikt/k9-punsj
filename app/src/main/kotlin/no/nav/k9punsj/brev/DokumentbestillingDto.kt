package no.nav.k9punsj.brev

import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.JsonB
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.NorskIdentDto

data class DokumentbestillingDto(
    val journalpostId: JournalpostIdDto,
    val brevId: BrevId? = null,
    val saksnummer: String? = null,
    val soekerId: NorskIdentDto,
    val mottaker: Mottaker,
    val fagsakYtelseType: FagsakYtelseType,
    val dokumentMal: String,
    val dokumentdata: JsonB? = null

) {
    data class Mottaker(
        val type : String,
        val id : String
    )
}
