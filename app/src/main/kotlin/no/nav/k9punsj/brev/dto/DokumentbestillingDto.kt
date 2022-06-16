package no.nav.k9punsj.brev.dto

import no.nav.k9.formidling.kontrakt.kodeverk.FagsakYtelseType
import java.util.*

data class DokumentbestillingDto(
    val journalpostId: String?,
    val brevId: String = UUID.randomUUID().toString(),
    val saksnummer: String,
    val aktørId: String,
    val overstyrtMottaker: MottakerDto,
    val ytelseType: FagsakYtelseType,
    val dokumentMal: String,
    val dokumentdata: BrevDataDto? = null
    )