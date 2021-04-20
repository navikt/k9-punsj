package no.nav.k9punsj.akjonspunkter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class AksjonspunktKode(override val kode: String, override val navn: String, val type: AksjonspunktType) : Kodeverdi {
    PUNSJ("PUNSJ", "Punsj oppgave", AksjonspunktType.AKSJONSPUNKT),
    PUNSJ_HAR_UTLØPT("UTLØPT", "Utløpt oppgave", AksjonspunktType.AKSJONSPUNKT),
    VENTER_PÅ_INFORMASJON("MER_INFORMASJON", "Venter på informasjon", AksjonspunktType.VENTEPUNKT);

    override val kodeverk = "PUNSJ_OPPGAVE_STATUS"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): AksjonspunktKode = values().find { it.kode == kode }!!
    }
}
