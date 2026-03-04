package no.nav.k9punsj.brev.dto

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import java.util.*

data class DokumentbestillingDto(
    val journalpostId: String?,
    val brevId: String? = UUID.randomUUID().toString(),
    @param:JsonSetter(nulls = Nulls.SKIP)
    val saksnummer: String = "GENERELL_SAK",
    val soekerId: String,
    val mottaker: MottakerDto,
    val fagsakYtelseType: FagsakYtelseType,
    val dokumentMal: String,
    val dokumentdata: BrevDataDto? = null
)
