package no.nav.k9punsj.akjonspunkter


enum class VentÅrsakType (val kode: String, val navn: String) {
    VENT_TRENGER_FLERE_OPPLYSINGER("V_FLERE_OPPLYSINGER", "Etterlysere flere opplysninger, venter på de.");

    companion object {
        fun fraKode(kode: String): VentÅrsakType = values().find { it.kode == kode }!!
    }
}

