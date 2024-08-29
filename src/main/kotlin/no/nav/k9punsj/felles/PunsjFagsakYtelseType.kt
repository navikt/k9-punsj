package no.nav.k9punsj.felles

enum class PunsjFagsakYtelseType(val kode: String, val navn: String, val oppgavetema: String?) {
    PLEIEPENGER_SYKT_BARN("PSB", "Pleiepenger sykt barn", "OMS"),
    PLEIEPENGER_LIVETS_SLUTTFASE("PPN", "Pleiepenger i livets sluttfase", "OMS"),
    OMSORGSPENGER("OMP", "Omsorgspenger", "OMS"),
    OMSORGSPENGER_UTBETALING("OMP_UT", "Omsorgspenger", "OMS"),
    OMSORGSPENGER_KRONISK_SYKT_BARN("OMP_KS", "Omsorgspenger kronisk sykt barn", "OMS"),
    OMSORGSPENGER_MIDLERTIDIG_ALENE("OMP_MA", "Ekstra omsorgsdager midlertidig alene", "OMS"),
    OMSORGSPENGER_ALENE_OMSORGEN("OMP_AO", "Alene om omsorgen",  "OMS"),
    OPPLÆRINGSPENGER("OLP", "Opplæringspenger", "OMS"),
    UKJENT("UKJENT", "Ukjent", null),
    UDEFINERT("-", "Ikke definert", null);

    companion object {
        private val map = entries.associateBy { v -> v.kode }
        fun fromKode(kode: String): PunsjFagsakYtelseType {

            return map[kode] ?: UKJENT
        }

        fun fraNavn(navn: String): PunsjFagsakYtelseType {
            return values().firstOrNull { it.name == navn } ?: UDEFINERT
        }
    }
}
