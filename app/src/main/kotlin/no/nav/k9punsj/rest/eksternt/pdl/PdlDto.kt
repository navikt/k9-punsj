package no.nav.k9punsj.rest.eksternt.pdl

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