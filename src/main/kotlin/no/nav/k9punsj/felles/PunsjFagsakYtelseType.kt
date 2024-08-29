package no.nav.k9punsj.felles

import no.nav.k9.kodeverk.behandling.FagsakYtelseType

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

    fun somK9FagsakYtelseType(): FagsakYtelseType {
        return when (this) {
            PLEIEPENGER_SYKT_BARN -> FagsakYtelseType.PLEIEPENGER_SYKT_BARN
            PLEIEPENGER_LIVETS_SLUTTFASE -> FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE
            OMSORGSPENGER -> FagsakYtelseType.OMSORGSPENGER
            OMSORGSPENGER_UTBETALING -> FagsakYtelseType.OMSORGSPENGER
            OMSORGSPENGER_KRONISK_SYKT_BARN -> FagsakYtelseType.OMSORGSPENGER_KS
            OMSORGSPENGER_MIDLERTIDIG_ALENE -> FagsakYtelseType.OMSORGSPENGER_MA
            OMSORGSPENGER_ALENE_OMSORGEN -> FagsakYtelseType.OMSORGSPENGER_AO
            OPPLÆRINGSPENGER -> FagsakYtelseType.OPPLÆRINGSPENGER
            UKJENT -> FagsakYtelseType.UDEFINERT
            else -> throw IllegalStateException("Ikke støttet fagsakytelsetype: $this")
        }
    }

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
