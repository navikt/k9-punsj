package no.nav.k9punsj.journalpost.dto

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.Companion.somPunsjFagsakYtelseType
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.integrasjoner.k9sak.dto.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.journalpost.JournalpostkopieringService
import org.springframework.http.HttpStatus
import java.time.LocalDate

data class KopierJournalpostDto(
    val til: String,
    val barn: String?,
    val annenPart: String?,
    val behandlingsÅr: Int?,
    val ytelse: PunsjFagsakYtelseType?
) {
    init {
        when {
            ytelse == null && barn.isNullOrBlank() && annenPart.isNullOrBlank() -> {
                throw JournalpostkopieringService.KanIkkeKopieresErrorResponse(
                    "Må sette minst barn eller annenPart",
                    HttpStatus.BAD_REQUEST
                )
            }

            ytelse != null && gjelderYtelseMedBarn() && barn.isNullOrBlank() && annenPart.isNullOrBlank() -> {
                throw JournalpostkopieringService.KanIkkeKopieresErrorResponse(
                    "Må sette minst barn eller annenPart",
                    HttpStatus.BAD_REQUEST
                )
            }

            ytelse != null && gjelderYtelseMedBehandlingsÅr() && behandlingsÅr == null -> {
                throw JournalpostkopieringService.KanIkkeKopieresErrorResponse(
                    "Må sette behandlingsÅr",
                    HttpStatus.BAD_REQUEST
                )
            }
        }
    }

    private fun gjelderYtelseMedBarn(): Boolean = listOf(
        PunsjFagsakYtelseType.PLEIEPENGER_SYKT_BARN,
        PunsjFagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE,
        PunsjFagsakYtelseType.OMSORGSPENGER_KRONISK_SYKT_BARN,
        PunsjFagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN,
        PunsjFagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE,
    ).contains(ytelse)

    private fun gjelderYtelseMedBehandlingsÅr(): Boolean = listOf(
        PunsjFagsakYtelseType.OMSORGSPENGER_UTBETALING,
        PunsjFagsakYtelseType.OMSORGSPENGER
    ).contains(ytelse)

    fun somK9SakGrunnlag(k9FagsakYtelseType: FagsakYtelseType, periodeDto: PeriodeDto?): HentK9SaksnummerGrunnlag {
        val periode = if (ytelse == null || !gjelderYtelseMedBehandlingsÅr()) periodeDto
        else PeriodeDto(LocalDate.of(behandlingsÅr!!, 1, 1), LocalDate.of(behandlingsÅr, 12, 31))

        return HentK9SaksnummerGrunnlag(
            søknadstype = ytelse ?: k9FagsakYtelseType.somPunsjFagsakYtelseType(),
            søker = til.somIdentitetsnummer().toString(),
            pleietrengende = barn?.somIdentitetsnummer().toString(),
            annenPart = annenPart?.somIdentitetsnummer().toString(),
            periode = periode
        )
    }
}
