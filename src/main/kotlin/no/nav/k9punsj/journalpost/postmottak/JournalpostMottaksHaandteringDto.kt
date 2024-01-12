package no.nav.k9punsj.journalpost.postmottak

import no.nav.k9punsj.felles.dto.PeriodeDto

data class JournalpostMottaksHaandteringDto(
    val brukerIdent: String,
    val barnIdent: String?,
    val annenPart: String?,
    val journalpostId: String,
    val fagsakYtelseTypeKode: String,
    val periode: PeriodeDto?,
    val saksnummer: String?
)
