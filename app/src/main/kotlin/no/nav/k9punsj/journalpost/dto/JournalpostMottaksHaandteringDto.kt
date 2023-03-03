package no.nav.k9punsj.journalpost.dto

import no.nav.k9punsj.felles.dto.PeriodeDto

internal data class JournalpostMottaksHaandteringDto(
    val brukerIdent: String,
    val barnIdent: String?,
    val annenPart: String?,
    val journalpostId: String,
    val fagsakYtelseTypeKode: String,
    val periode: PeriodeDto?,
    val saksnummer: String?
)
