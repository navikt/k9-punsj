package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.PeriodeDto

@Deprecated("Erstattes med bedre løsning v.h.a. k9-format?")
data class HentK9SaksnummerGrunnlag(
    val søknadstype: FagsakYtelseType,
    val søker: String,
    val pleietrengende: String?,
    val annenPart: String?,
    val periode: PeriodeDto?,
)
