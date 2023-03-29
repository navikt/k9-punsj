package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.felles.FagsakYtelseType

data class HentK9SaksnummerGrunnlag(
    internal val søknadstype: FagsakYtelseType,
    internal val søker: String,
    internal val pleietrengende: String?,
    internal val annenPart: String?,
    internal val journalpostId: String
)
