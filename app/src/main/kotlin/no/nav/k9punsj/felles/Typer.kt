package no.nav.k9punsj.felles

import no.nav.k9.kodeverk.behandling.FagsakYtelseType

internal data class AktørId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugyldig aktørId" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{5,40}".toRegex()
        internal fun String.somAktørId() = AktørId(this)
    }
}

internal data class JournalpostId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugyldig journalpostId" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{5,40}".toRegex()
        internal fun String.somJournalpostId() = JournalpostId(this)
    }
}

internal data class Identitetsnummer private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "Ugyldig identitetsnummer" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{11,25}".toRegex()
        internal fun String.somIdentitetsnummer() = Identitetsnummer(this)
    }
}

internal data class CorrelationId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugyldig correlation id" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "[a-zA-Z0-9_.\\-æøåÆØÅ]{5,200}".toRegex()
        internal fun String.somCorrelationId() = CorrelationId(this)
    }
}

internal data class K9Saksnummer private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er et ugyldig K9 saksnummer" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "[A-Za-z0-9]{5,20}".toRegex()
        internal fun String.somK9Saksnummer() = K9Saksnummer(this)
    }
}

enum class PunsjJournalpostKildeType(val kode: String, val navn: String) {
    FORDEL("FORDEL", "Fordel"),
    SAKSBEHANDLER("SAKSBEHANDLER", "Saksbehandler");
}

@Deprecated("Bytt til intern funksjonalitet i punsj")
data class PunsjBolleDto(
    val brukerIdent: String,
    // todo bytt navn til pleietrengende
    val barnIdent: String?,
    val annenPart: String?,
    val journalpostId: String,
    val fagsakYtelseType: FagsakYtelseType
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
