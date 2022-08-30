package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.Identitetsnummer
import java.time.LocalDate

data class LopendeSakDto(
    val søker: Identitetsnummer,
    val pleietrengende: Identitetsnummer?,
    val annenPart: Identitetsnummer?,
    val fraOgMed: LocalDate,
    val fagsakYtelseType: FagsakYtelseType
)
