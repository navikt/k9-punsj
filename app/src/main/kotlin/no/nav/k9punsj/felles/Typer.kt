package no.nav.k9punsj.felles

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.integrasjoner.dokarkiv.Sak

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

enum class PunsjJournalpostKildeType(val kode: String, val navn: String) {
    FORDEL("FORDEL", "Fordel"),
    SAKSBEHANDLER("SAKSBEHANDLER", "Saksbehandler");
}

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

data class LukkJournalpostDto(
    val norskIdent: String,
    val sak: Sak
)

data class IdentDto(
    val norskIdent: String
)
