package no.nav.k9punsj.journalpost.postmottak

import no.nav.k9punsj.felles.dto.PeriodeDto

data class JournalpostMottaksHaandteringDto(
    val journalpostId: String,
    val brukerIdent: String,
    val fagsakYtelseTypeKode: String,
    val saksnummer: String?,
    val barnIdent: String?, // TODO: Trenges ikke?
    val annenPart: String?,// TODO: Trenges ikke?
    val periode: PeriodeDto?,// TODO: Trenges ikke?
)
