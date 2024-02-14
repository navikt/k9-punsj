package no.nav.k9punsj.journalpost.dto

import no.nav.k9punsj.felles.dto.PeriodeDto

data class Sak(
    val fagsakId: String?,
    val sakstype: String?,
    val gyldigPeriode: PeriodeDto?,
    val pleietrengendeIdent: String?,
)
