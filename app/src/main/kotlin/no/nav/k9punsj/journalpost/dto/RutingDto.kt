package no.nav.k9punsj.journalpost.dto

import no.nav.k9punsj.felles.dto.PeriodeDto

data class RutingDto(
    val brukerIdent: String,
    val pleietrengende: String?,
    val annenPart: String?,
    val journalpostId: String,
    val fagsakYtelseType: no.nav.k9.kodeverk.behandling.FagsakYtelseType,
    val periode: PeriodeDto? = null // Støtte for periode for o overstyre utleding som sker m.h.a journalpost-metadata.
)
