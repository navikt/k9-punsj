package no.nav.k9punsj.journalpost

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.domenetjenester.dto.AktørIdDto
import no.nav.k9punsj.domenetjenester.dto.JournalpostIdDto
import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto
import no.nav.k9punsj.domenetjenester.dto.SøknadIdDto

internal data class JournalpostId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugylidig journalpostId" } }
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
    FORDEL ("FORDEL", "Fordel"),
    SAKSBEHANDLER("SAKSBEHANDLER", "Saksbehandler");
}


data class PunsjBolleDto(
    val brukerIdent: NorskIdentDto,
    //todo bytt navn til pleietrengende
    val barnIdent: NorskIdentDto?,
    val annenPart: NorskIdentDto?,
    val journalpostId: JournalpostIdDto,
    val fagsakYtelseType: FagsakYtelseType
)

data class IdentOgJournalpost(
    val norskIdent: NorskIdentDto,
    val journalpostId: JournalpostIdDto,
)

data class SøkUferdigJournalposter(
    val aktorIdentDto: AktørIdDto,
    val aktorIdentBarnDto: AktørIdDto?
)

data class SettPåVentDto(
    val soeknadId: SøknadIdDto?
)

data class IdentDto(
    val norskIdent: NorskIdentDto
)
