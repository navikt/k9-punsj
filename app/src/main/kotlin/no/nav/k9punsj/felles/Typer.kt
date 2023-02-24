package no.nav.k9punsj.felles

data class JournalpostId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugyldig journalpostId" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{5,40}".toRegex()
        internal fun String.somJournalpostId() = JournalpostId(this)
    }
}

data class Identitetsnummer private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "Ugyldig identitetsnummer" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{11,25}".toRegex()
        internal fun String.somIdentitetsnummer() = Identitetsnummer(this)
    }
}

data class IdentOgJournalpost(
    val norskIdent: String,
    val journalpostId: String
)

data class Sak(
    val sakstype: SaksType,
    val fagsakId: String? = null,
) {
    val fagsaksystem = if (sakstype == SaksType.FAGSAK) FagsakSystem.K9 else null
    init {
        when (sakstype) {
            SaksType.FAGSAK -> {
                require(fagsaksystem != null && !fagsakId.isNullOrBlank()) {
                    "Dersom sakstype er ${SaksType.FAGSAK}, så må fagsaksystem og fagsakId være satt. fagsaksystem=[$fagsaksystem], fagsakId=[$fagsakId]"
                }
            }
            SaksType.GENERELL_SAK -> {
                require(fagsaksystem == null && fagsakId.isNullOrBlank()) {
                    "Dersom sakstype er ${SaksType.GENERELL_SAK}, så kan ikke fagsaksystem og fagsakId være satt. fagsaksystem=[$fagsaksystem], fagsakId=[$fagsakId]"
                }
            }
            SaksType.ARKIVSAK -> throw UnsupportedOperationException("ARKIVSAK skal kun brukes etter avtale.")
        }
    }

    enum class SaksType { FAGSAK, GENERELL_SAK, ARKIVSAK }
    enum class FagsakSystem { K9 }

}
