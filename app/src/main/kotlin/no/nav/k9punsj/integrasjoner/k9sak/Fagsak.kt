package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9.kodeverk.behandling.FagsakYtelseType

data class Fagsak(
    val saksnummer: String,
    val sakstype: FagsakYtelseType
)
