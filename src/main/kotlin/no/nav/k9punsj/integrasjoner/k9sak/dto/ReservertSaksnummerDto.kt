package no.nav.k9punsj.integrasjoner.k9sak.dto

import no.nav.k9.kodeverk.behandling.FagsakYtelseType

data class ReservertSaksnummerDto(
    val saksnummer: String,
    val ytelseType: FagsakYtelseType,
    val brukerAktørId: String,
    val pleietrengendeAktørId: String? = null,
) {
    override fun toString(): String {
        return "HentReservertSaksnummerDto(saksnummer='$saksnummer', ytelseType=$ytelseType), harBrukerAktørId=${brukerAktørId.isNotBlank()}, harPleietrengendeAktørId=${!pleietrengendeAktørId.isNullOrBlank()}"
    }
}
