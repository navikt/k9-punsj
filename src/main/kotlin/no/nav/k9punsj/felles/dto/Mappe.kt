package no.nav.k9punsj.felles.dto

import no.nav.k9punsj.felles.PunsjFagsakYtelseType

data class Mappe(
    val mappeId: String,
    val søker: Person,
    val bunke: List<BunkeEntitet>
) {
    fun hentFor(punsjFagsakYtelseType: PunsjFagsakYtelseType): BunkeEntitet? {
        return bunke.firstOrNull { it.punsjFagsakYtelseType == punsjFagsakYtelseType }
    }
}
