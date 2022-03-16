package no.nav.k9punsj.db.datamodell

enum class FagsakYtelseType(val kode: String, val navn: String, val infotrygdBehandlingstema: String?, val oppgavetema: String?, val uri: String) {
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn", "PN", "OMS", FagsakYtelseTypeUri.PLEIEPENGER_SYKT_BARN),
    PLEIEPENGER_LIVETS_SLUTTFASE("PPN", "Pleiepenger i livets sluttfase", "PP", "OMS", FagsakYtelseTypeUri.PLEIEPENGER_LIVETS_SLUTTFASE),
    OMSORGSPENGER("OMP", "Omsorgspenger", "OM", "OMS", FagsakYtelseTypeUri.OMSORGSPENGER),
    OMSORGSPENGER_KRONISK_SYKT_BARN("OMP_KS", "Omsorgspenger kronisk sykt barn", "OM", "OMS", FagsakYtelseTypeUri.OMSORGSPENGER_KRONISK_SYKT_BARN),
    OMSORGSPENGER_MIDLERTIDIG_ALENE("OMP_MA", "Ekstra omsorgsdager midlertidig alene", "OM", "OMS", FagsakYtelseTypeUri.OMSORGSPENGER_MIDLERTIDIG_ALENE),
    OMSORGSPENGER_ALENE_OMSORGEN("OMP_AO", "Alene om omsorgen", "OM", "OMS", FagsakYtelseTypeUri.OMSORGSPENGER_ALENE_OM_OMSORGEN),
    UKJENT("UKJENT", "Ukjent", null, null, FagsakYtelseTypeUri.UKJENT);

    companion object {
        private val map = values().associateBy { v -> v.kode }
        fun fromKode(kode: String): FagsakYtelseType {
            val type = map[kode]
            if (type != null) {
                return type
            } else {
                return UKJENT
            }
        }
    }
}



