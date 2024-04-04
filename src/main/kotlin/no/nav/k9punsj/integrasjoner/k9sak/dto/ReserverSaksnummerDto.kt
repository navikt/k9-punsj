package no.nav.k9punsj.integrasjoner.k9sak.dto

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.kodeverk.behandling.FagsakYtelseType.*

data class ReserverSaksnummerDto(
    val brukerAktørId: String,
    val pleietrengendeAktørId: String? = null,
    val relatertPersonAktørId: String? = null,
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
