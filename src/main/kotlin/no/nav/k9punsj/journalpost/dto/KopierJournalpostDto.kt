package no.nav.k9punsj.journalpost.dto

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.Companion.somPunsjFagsakYtelseType
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.integrasjoner.k9sak.dto.HentK9SaksnummerGrunnlag

data class KopierJournalpostDto(
    val fra: String,
    val til: String,
    val barn: String?,
    val annenPart: String?,
    val ytelse: PunsjFagsakYtelseType?
) {
    init {
        require(barn != null || annenPart != null) {
            "Må sette minst en av barn og annenPart"
        }
    }

    fun somK9SakGrunnlag(k9FagsakYtelseType: FagsakYtelseType, periodeDto: PeriodeDto?): HentK9SaksnummerGrunnlag = HentK9SaksnummerGrunnlag(
        søknadstype = ytelse ?: k9FagsakYtelseType.somPunsjFagsakYtelseType(),
        søker = fra.somIdentitetsnummer().toString(),
        pleietrengende = barn?.somIdentitetsnummer().toString(),
        annenPart = annenPart?.somIdentitetsnummer().toString(),
        periode = periodeDto
    )
}
