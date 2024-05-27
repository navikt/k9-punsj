package no.nav.k9punsj.sak.dto

import no.nav.k9punsj.felles.dto.PeriodeDto

data class SakInfoDto(
    val reservert: Boolean,
    val fagsakId: String,
    val sakstype: String,
    val pleietrengendeIdent: String?,
    val gyldigPeriode: PeriodeDto?,
    val behandlingsår: Int?,
    val relatertPersonIdent: String?
)
