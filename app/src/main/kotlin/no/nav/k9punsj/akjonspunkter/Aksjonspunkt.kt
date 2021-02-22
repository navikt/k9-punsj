package no.nav.k9punsj.akjonspunkter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Aksjonspunkt(override val kode: String, override val navn: String) : Kodeverdi {
    PUNSJ("0001", "Punsj oppgave");

    override val kodeverk = "PUNSJ_OPPGAVE_STATUS"

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): Aksjonspunkt = values().find { it.kode == kode }!!
    }
}
