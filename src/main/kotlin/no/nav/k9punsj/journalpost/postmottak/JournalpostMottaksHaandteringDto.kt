package no.nav.k9punsj.journalpost.postmottak

data class JournalpostMottaksHaandteringDto(
    val journalpostId: String,
    val brukerIdent: String,
    val fagsakYtelseTypeKode: String,
    val saksnummer: String? // Settes kun ved tilknytning mot eksisterende sak.
)
