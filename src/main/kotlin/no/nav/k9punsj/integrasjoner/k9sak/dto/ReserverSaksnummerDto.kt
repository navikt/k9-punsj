package no.nav.k9punsj.integrasjoner.k9sak.dto

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.*
import no.nav.k9.sak.typer.AktørId

data class ReserverSaksnummerDto(
    val brukerAktørId: AktørId,
    val pleietrengendeAktørId: AktørId? = null,
    val relatertPersonAktørId: AktørId? = null,
    val barnAktørIder: List<AktørId> = listOf(),
    val ytelseType: FagsakYtelseType,
    var behandlingsår: Int? = null,
) {
    init {
       when(ytelseType) {
           OMSORGSPENGER, OMSORGSPENGER_KS, OMSORGSPENGER_MA, OMSORGSPENGER_AO -> {} // OK
           else -> behandlingsår = null
       }
    }
}
