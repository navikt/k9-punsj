package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9.kodeverk.behandling.FagsakYtelseType

data class ReserverSaksnummerDto(
    val brukerAktørId: String,
    val pleietrengendeAktørId: String? = null,
    val ytelseType: FagsakYtelseType,
)
