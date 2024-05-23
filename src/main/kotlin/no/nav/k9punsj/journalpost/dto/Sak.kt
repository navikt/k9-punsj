package no.nav.k9punsj.journalpost.dto

import no.nav.k9punsj.felles.dto.PeriodeDto

data class Sak(
    val reservertSaksnummer: Boolean,
    val fagsakId: String?,
    val gyldigPeriode: PeriodeDto?,
    val pleietrengendeIdent: String?,
    val relatertPersonIdent: String?,
    val sakstype: String?,
    val behandlingsÅr: Int?,
) {
    override fun toString(): String {
        return "Sak(reservertSaksnummer=$reservertSaksnummer, fagsakId=$fagsakId, sakstype=$sakstype, gyldigPeriode=$gyldigPeriode, harPleietrengendeIdent=${!pleietrengendeIdent.isNullOrBlank()}, harRelatertPersonIdent=${!relatertPersonIdent.isNullOrBlank()})"
    }
}
