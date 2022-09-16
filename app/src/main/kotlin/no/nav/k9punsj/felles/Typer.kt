package no.nav.k9punsj.felles

import no.nav.k9.kodeverk.behandling.FagsakYtelseType

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

data class IdentDto(
    val norskIdent: String
)
