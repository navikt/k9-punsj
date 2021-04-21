package no.nav.k9punsj.akjonspunkter

enum class AksjonspunktStatus(val kode: String, val navn: String) {
    AVBRUTT ("AVBR", "Avbrutt"),
    OPPRETTET("OPPR", "Opprettet"),
    UTFØRT ("UTFO", "Utført");


    companion object {
        fun fraKode(kode: String): AksjonspunktStatus = values().find { it.kode == kode }!!
    }
}
