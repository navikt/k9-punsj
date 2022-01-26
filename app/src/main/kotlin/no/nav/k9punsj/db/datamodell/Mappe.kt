package no.nav.k9punsj.db.datamodell


typealias MappeId = String

data class Mappe(
    val mappeId: MappeId,
    val søker: Person,
    val bunke: List<BunkeEntitet>,
) {
    fun hentFor(fagsakYtelseType: FagsakYtelseType) : BunkeEntitet? {
        return bunke.firstOrNull { it.fagsakYtelseType == fagsakYtelseType }
    }
}
