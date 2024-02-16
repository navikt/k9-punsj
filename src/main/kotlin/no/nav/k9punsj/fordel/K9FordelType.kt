package no.nav.k9punsj.fordel

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

/*
*
* Dette er en kontrakt som settes av K9-Fordel
* https://github.com/navikt/k9-fordel/blob/323140f23a7ee80e32943fbaad8081265478b8b1/fordel/domene/src/main/java/no/nav/k9/domenetjenester/punsj/PunsjInnsendingType.java
*
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class K9FordelType(val kode: String, val navn: String, val erScanning: Boolean) {
    PAPIRSØKNAD("PAPIRSØKNAD", "Papirsøknad", true),
    DIGITAL_SØKNAD("DIGITAL_SØKNAD", "Digital søknad", false),
    PAPIRETTERSENDELSE("PAPIRETTERSENDELSE", "Papirettersendelse", true),
    PAPIRINNTEKTSOPPLYSNINGER("PAPIRINNTEKTSOPPLYSNINGER", "Papirinntektsopplysninger", true),
    DIGITAL_ETTERSENDELSE("DIGITAL_ETTERSENDELSE", "Digital ettersendelse", false),
    INNLOGGET_CHAT("INNLOGGET_CHAT", "Innlogget chat", false),
    SKRIV_TIL_OSS_SPØRMSÅL("SKRIV_TIL_OSS_SPØRMSÅL", "Skriv til oss spørmsål", false),
    SKRIV_TIL_OSS_SVAR("SKRIV_TIL_OSS_SVAR", "Skriv til oss svar", false),
    SAMTALEREFERAT("SAMTALEREFERAT", "Samtalereferat", false),
    INNTEKTSMELDING_UTGÅTT("INNTEKTSMELDING_UTGÅTT", "inntektsmelding utgått", false),
    PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG("PUNSJOPPGAVE_IKKE_LENGER_NØDVENDIG", "Punsjoppgave ikke lenger nødvendig", false),
    UTEN_FNR_DNR("UTEN_FNR_DNR", "Uten fnr eller dnr", false),
    KOPI("KOPI", "Kopi", true),
    UKJENT("UKJENT", "Ukjent", true);

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): K9FordelType = values().find {
            it.kode == kode
        } ?: UKJENT

        fun sjekkOmDetErScanning(kode: String): Boolean {
            return valueOf(kode).erScanning
        }
    }
}
