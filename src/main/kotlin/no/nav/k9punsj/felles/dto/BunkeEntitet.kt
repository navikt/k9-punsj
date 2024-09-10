package no.nav.k9punsj.felles.dto

import no.nav.k9punsj.felles.PunsjFagsakYtelseType

data class BunkeEntitet(
    val bunkeId: String,
    val punsjFagsakYtelseType: PunsjFagsakYtelseType,
    val søknader: List<SøknadEntitet>? = listOf()
)
