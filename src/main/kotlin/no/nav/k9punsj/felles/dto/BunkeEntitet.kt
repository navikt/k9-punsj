package no.nav.k9punsj.felles.dto

import no.nav.k9punsj.felles.FagsakYtelseType

data class BunkeEntitet(
    val bunkeId: String,
    val fagsakYtelseType: FagsakYtelseType,
    val søknader: List<SøknadEntitet>? = listOf()
)
