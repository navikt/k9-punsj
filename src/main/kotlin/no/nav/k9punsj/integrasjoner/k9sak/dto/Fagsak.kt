package no.nav.k9punsj.integrasjoner.k9sak.dto

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.felles.dto.PeriodeDto

data class Fagsak(
    val saksnummer: String,
    val sakstype: FagsakYtelseType,
    val pleietrengendeAktorId: String?,
    val gyldigPeriode: PeriodeDto?
)
