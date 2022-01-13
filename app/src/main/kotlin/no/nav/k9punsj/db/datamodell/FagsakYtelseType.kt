package no.nav.k9punsj.db.datamodell

enum class FagsakYtelseType(val kode: String, val navn: String, val infotrygdBehandlingstema: String?, val oppgavetema: String?, val uri: String) {
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn", "PN", "OMS", FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN),
    OMSORGSPENGER("OMP", "Omsorgspenger", "OM", "OMS", FagsakYtelseTypeUri.OMSORGSPENGER),
    OMSORGSPENGER_KRONISK_SYKT_BARN("OMP_KSB", "Omsorgspenger kronisk sykt barn", "OM", "OMS", FagsakYtelseTypeUri.OMSORGSPENGER_KRONISK_SYKT_BARN),
    PLEIEPENGER_NÆRSTÅENDE("PPN", "Pleiepenger nærstående", null, "OMS", FagsakYtelseTypeUri.PLEIEPENGER_NÆRSTÅENDE),
    UKJENT("UKJENT", "Ukjent", null, null, FagsakYtelseTypeUri.UKJENT);

    companion object {
        private val map = values().associateBy { v -> v.kode }
        fun fromKode(kode: String): FagsakYtelseType {
            val type = map[kode]
            if (type != null) {
                return type
            } else {
                throw IllegalStateException("Fant ingen FagsakYtelseType med koden $kode")
            }
        }
    }
}



