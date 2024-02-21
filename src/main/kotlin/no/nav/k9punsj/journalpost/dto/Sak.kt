package no.nav.k9punsj.journalpost.dto

import no.nav.k9punsj.felles.dto.PeriodeDto

data class Sak(
    val reservertSaksnummer: Boolean,
    val fagsakId: String?,
    val gyldigPeriode: PeriodeDto?,
    val pleietrengendeIdent: String?,
    val sakstype: String?,
)
