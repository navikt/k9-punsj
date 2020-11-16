package no.nav.k9.akjonspunkter

enum class AksjonspunktStatus(val kode: String, val navn: String) {
    AVBRUTT ("AVBR", "Avbrutt"),
    OPPRETTET("OPPR", "Opprettet"),
    UTFØRT ("UTFO", "Utført");

}
