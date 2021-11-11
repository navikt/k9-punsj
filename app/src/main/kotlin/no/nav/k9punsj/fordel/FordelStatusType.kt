package no.nav.k9punsj.fordel

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class FordelStatusType(val kode: String) {
    OPPRETTET("OPPRETTET"),
    LUKKET_FRA_FORDEL("LUKKET_FRA_FORDEL");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraKode(kode: String): FordelStatusType = values().find { it.kode == kode } ?: throw IllegalStateException("Finner ikke$kode")

    }
}
