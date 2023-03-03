package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.felles.FagsakYtelseType
import java.time.LocalDate

data class LopendeSakDto(
    val søker: String,
    val pleietrengende: String?,
    val annenPart: String?,
    val fraOgMed: LocalDate,
    val fagsakYtelseType: FagsakYtelseType
)
