package no.nav.k9punsj.fordel

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class PunsjInnsendingType(val kode: String, val navn: String, val erScanning: Boolean) {
    PAPIRSØKNAD("PAPIRSØKNAD", "Papirsøknad", true),
    PAPIRETTERSENDELSE("PAPIRETTERSENDELSE", "Papirettersendelse",true),
    PAPIRINNTEKTSOPPLYSNINGER("PAPIRINNTEKTSOPPLYSNINGER", "Papirinntektsopplysninger",true),
    DIGITAL_ETTERSENDELSE("DIGITAL_ETTERSENDELSE", "Digital ettersendelse",false),
    INNLOGGET_CHAT("INNLOGGET_CHAT", "Innlogget chat",false),
    SKRIV_TIL_OSS_SPØRMSÅL("SKRIV_TIL_OSS_SPØRMSÅL", "Skriv til oss spørmsål",false),
    SKRIV_TIL_OSS_SVAR("SKRIV_TIL_OSS_SVAR", "Skriv til oss svar",false),
    SAMTALEREFERAT("SAMTALEREFERAT", "Samtalereferat",false),
    INNTEKTSMELDING_UTGÅTT("INNTEKTSMELDING_UTGÅTT", "inntektsmelding utgått", false),
    PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG("PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG", "Punsjoppgave ikke lenger nødvendig", false),
    UTEN_FNR_DNR("UTEN_FNR_DNR", "Uten fnr eller dnr", false),
    KOPI("KOPI", "Kopi",true),
    UKJENT("UKJENT", "Ukjent",true);

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): PunsjInnsendingType = values().find { it.kode == kode } ?: throw IllegalStateException("Finner ikke $kode")

        fun sjekkOmDetErScanning(kode: String): Boolean {
            return valueOf(kode).erScanning
        }
    }
}
