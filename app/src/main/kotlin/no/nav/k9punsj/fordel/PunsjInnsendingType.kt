package no.nav.k9punsj.fordel

enum class PunsjInnsendingType(val kode: String, val erScanning: Boolean) {
    PAPIRSØKNAD("PAPIRSØKNAD", true),
    PAPIRETTERSENDELSE("PAPIRETTERSENDELSE", true),
    PAPIRINNTEKTSOPPLYSNINGER("PAPIRINNTEKTSOPPLYSNINGER", true),
    DIGITAL_ETTERSENDELSE("DIGITAL_ETTERSENDELSE", false),
    INNLOGGET_CHAT("INNLOGGET_CHAT", false),
    SKRIV_TIL_OSS_SPØRMSÅL("SKRIV_TIL_OSS_SPØRMSÅL", false),
    SKRIV_TIL_OSS_SVAR("SKRIV_TIL_OSS_SVAR", false),
    UKJENT("UKJENT", true);


    companion object {
        fun sjekkOmDetErScanning(kode: String) : Boolean {
            return valueOf(kode).erScanning
        }
    }
}
