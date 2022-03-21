package no.nav.k9punsj.integrasjoner.pdl

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import java.time.LocalDate

data class IdentPdl(
    val data: Data,
    val errors: List<Error>?
) {
    data class Data(
            var hentIdenter: HentIdenter?
    ) {
        data class HentIdenter(
                val identer: List<Identer>
        ) {
            data class Identer(
                    val gruppe: String,
                    val historisk: Boolean,
                    var ident: String
            )
        }
    }
    data class Error(
        val extensions: Extensions,
        val locations: List<Location>,
        val message: String,
        val path: List<String>
    ) {
        data class Extensions(
                val classification: String,
                val code: String
        )

        data class Location(
                val column: Int,
                val line: Int
        )
    }
}

data class PdlResponse(
        val ikkeTilgang: Boolean,
        val identPdl: IdentPdl?
)


data class PersonPdl(
        val `data`: Data
) {
    data class Data(
            val hentPerson: HentPerson
    ) {
        data class HentPerson(
            val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
            val navn: List<Navn>,
            val kjoenn: List<Kjoenn>,
            val doedsfall:List<Doedsfall>
        ) {
            data class Kjoenn(
                    val kjoenn: String
            )

            data class Doedsfall(
                    @JsonSerialize(using = ToStringSerializer::class)
                    @JsonDeserialize(using = LocalDateDeserializer::class)
                    val doedsdato: LocalDate
            )

            data class Folkeregisteridentifikator(
                    val identifikasjonsnummer: String
            )

            data class Navn(
                    val etternavn: String,
                    val forkortetNavn: String?,
                    val fornavn: String,
                    val mellomnavn: String?
            )
        }
    }
}

internal fun PersonPdl.navn(): String{
    return data.hentPerson.navn[0].forkortetNavn
        ?: (data.hentPerson.navn[0].fornavn + " " + data.hentPerson.navn[0].etternavn)
}
