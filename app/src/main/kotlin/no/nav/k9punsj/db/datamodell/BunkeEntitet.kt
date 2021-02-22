package no.nav.k9punsj.db.datamodell

typealias BunkeId = String

data class BunkeEntitet(
    val bunkeId: BunkeId,
    val fagsakYtelseType: FagsakYtelseType,
    val søknader: List<SøknadEntitet>? = listOf(),
)
