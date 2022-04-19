package no.nav.k9punsj.db.datamodell

data class BunkeEntitet(
    val bunkeId: String,
    val fagsakYtelseType: FagsakYtelseType,
    val søknader: List<SøknadEntitet>? = listOf(),
)
