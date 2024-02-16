package no.nav.k9punsj.journalpost.postmottak

data class JournalpostMottaksHaandteringDto(
    val journalpostId: String,
    val brukerIdent: String,
    val barnIdent: String?, // Settes kun ved tilknytning mot reservert saksnummer.
    val fagsakYtelseTypeKode: String,
    val saksnummer: String? // Settes kun ved tilknytning mot eksisterende sak.
)
