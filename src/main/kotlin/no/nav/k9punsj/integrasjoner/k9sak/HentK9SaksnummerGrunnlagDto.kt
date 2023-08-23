package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.felles.FagsakYtelseType

data class HentK9SaksnummerGrunnlag(
    val søknadstype: FagsakYtelseType,
    val søker: String,
    val pleietrengende: String?,
    val annenPart: String?,
    val journalpostId: String
)
