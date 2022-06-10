package no.nav.k9punsj.brev.dto

enum class BrevType(val kode: String, val navn: String) {
    FRITEKSTBREV("FRITEKSTBREV", "Fritekstbrev");

    companion object {
        private val map = values().associateBy { v -> v.kode }
        fun fromKode(kode: String): BrevType {
            val type = map[kode]
            if (type != null) {
                return type
            } else {
                throw IllegalStateException("Fant ingen BrevType med koden $kode")
            }
        }
    }
}
