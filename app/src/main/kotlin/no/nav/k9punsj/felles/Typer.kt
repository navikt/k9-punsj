package no.nav.k9punsj.felles

data class AktørId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugyldig aktørId" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{5,40}".toRegex()
        internal fun String.somAktørId() = AktørId(this)
    }
}

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

data class CorrelationId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugyldig correlation id" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "[a-zA-Z0-9_.\\-æøåÆØÅ]{5,200}".toRegex()
        internal fun String.somCorrelationId() = CorrelationId(this)
    }
}

enum class PunsjJournalpostKildeType(val kode: String, val navn: String) {
    FORDEL("FORDEL", "Fordel"),
    SAKSBEHANDLER("SAKSBEHANDLER", "Saksbehandler");
}

data class RutingDto(
    val brukerIdent: String,
    val pleietrengende: String?,
    val annenPart: String?,
    val journalpostId: String,
    val fagsakYtelseType: String
)

data class IdentOgJournalpost(
    val norskIdent: String,
    val journalpostId: String
)

data class SøkUferdigJournalposter(
    val aktorIdentDto: String,
    val aktorIdentBarnDto: String?
)

data class SettPåVentDto(
    val soeknadId: String?
)

data class IdentDto(
    val norskIdent: String
)
