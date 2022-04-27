package no.nav.k9punsj.felles

enum class FagsakYtelseType(val kode: String, val navn: String, val infotrygdBehandlingstema: String?, val oppgavetema: String?) {
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn", "PN", "OMS"),
    PLEIEPENGER_LIVETS_SLUTTFASE("PPN", "Pleiepenger i livets sluttfase", "PP", "OMS"),
    OMSORGSPENGER("OMP", "Omsorgspenger", "OM", "OMS"),
    OMSORGSPENGER_KRONISK_SYKT_BARN("OMP_KS", "Omsorgspenger kronisk sykt barn", "OM", "OMS"),
    OMSORGSPENGER_MIDLERTIDIG_ALENE("OMP_MA", "Ekstra omsorgsdager midlertidig alene", "OM", "OMS"),
    OMSORGSPENGER_ALENE_OMSORGEN("OMP_AO", "Alene om omsorgen", "OM", "OMS"),
    OMSORGSPENGERUTBETALING("OMP_UT", "Omsorgspengerutbetaling", "OM", "OMS"),
    UKJENT("UKJENT", "Ukjent", null, null),
    UDEFINERT("-", "Ikke definert", null, null);

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



