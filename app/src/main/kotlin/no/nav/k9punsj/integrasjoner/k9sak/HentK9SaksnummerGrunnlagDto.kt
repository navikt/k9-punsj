package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.Periode

data class HentK9SaksnummerGrunnlag(
    internal val søknadstype: FagsakYtelseType,
    internal val søker: String,
    internal val pleietrengende: String?,
    internal val annenPart: String?,
    internal val periode: Periode
) {
    override fun toString(): String {
        return "HentK9SaksnummerGrunnlag(søknadstype=$søknadstype, søker=***, pleietrengende=${pleietrengende?.let { "***" }}, annenPart=${annenPart?.let { "***" }}, periode=$periode)"
    }
}
