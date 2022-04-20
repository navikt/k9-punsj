package no.nav.k9punsj.db.datamodell

data class Mappe(
    val mappeId: String,
    val søker: Person,
    val bunke: List<BunkeEntitet>,
) {
    fun hentFor(fagsakYtelseType: FagsakYtelseType) : BunkeEntitet? {
        return bunke.firstOrNull { it.fagsakYtelseType == fagsakYtelseType }
    }
}
