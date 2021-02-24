package no.nav.k9punsj.rest.web.dto

import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto.*
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto.PleiepengerYtelseDto.*
import java.time.LocalDate

data class PleiepengerSøknadMottakDto(

    val søker: SøkerDto?,
    val mottattDato: LocalDate?,
    val ytelse: PleiepengerYtelseDto?,
) {

    data class PleiepengerYtelseDto(
        val barn: BarnDto?,
        val søknadsperiode: String?,
        val arbeidAktivitet: ArbeidAktivitetDto?,
        val dataBruktTilUtledning: DataBruktTilUtledningDto?,
        val bosteder: BostederDto?,
        val utenlandsopphold: UtenlandsoppholdDto?,
        val beredskap: BeredskapDto?,
        val nattevåk: NattevåkDto?,
        val tilsynsordning: TilsynsordningDto?,
        val lovbestemtFerie: LovbestemtFerieDto?,
        val arbeidstid: ArbeidstidDto?,
        val uttak: UttakDto?,
        val omsorg: OmsorgDto?,
    ) {
        data class ArbeidAktivitetDto(
            val selvstendigNæringsdrivende: List<SelvstendigNæringsdrivendeDto>?,
            val frilanser: PleiepengerSøknadVisningDto.PleiepengerYtelseDto.ArbeidAktivitetDto.FrilanserDto?,
            val arbeidstaker: List<ArbeidstakerDto>?,
        ) {
            data class SelvstendigNæringsdrivendeDto(
                val perioder: Map<String, PleiepengerSøknadVisningDto.PleiepengerYtelseDto.ArbeidAktivitetDto.SelvstendigNæringsdrivendeDto.SelvstendigNæringsdrivendePeriodeInfoDto>?,
                val organisasjonsnummer: String?,
                val virksomhetNavn: String?,
            )

            data class ArbeidstakerDto(
                val norskIdentitetsnummer: NorskIdentDto?,
                val organisasjonsnummer: String?,
                val arbeidstidInfo: ArbeidstidInfoDto,
            ) {
                data class ArbeidstidInfoDto(
                    val jobberNormaltTimerPerDag: String?,
                    val perioder: Map<String, PleiepengerSøknadVisningDto.PleiepengerYtelseDto.ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto>?,
                )
            }
        }

        data class BostederDto(
            val perioder: Map<String, PleiepengerSøknadVisningDto.PleiepengerYtelseDto.BostederDto.BostedPeriodeInfoDto>?,
        )

        data class UtenlandsoppholdDto(
            val perioder: Map<String, PleiepengerSøknadVisningDto.PleiepengerYtelseDto.UtenlandsoppholdDto.UtenlandsoppholdPeriodeInfoDto>?,

            )

        data class BeredskapDto(
            val perioder: Map<String, PleiepengerSøknadVisningDto.PleiepengerYtelseDto.BeredskapDto.BeredskapPeriodeInfoDto>?,

            )

        data class NattevåkDto(
            val perioder: Map<String, PleiepengerSøknadVisningDto.PleiepengerYtelseDto.NattevåkDto.NattevåkPeriodeInfoDto>?,
        )

        data class TilsynsordningDto(
            val perioder: Map<String, PleiepengerSøknadVisningDto.PleiepengerYtelseDto.TilsynsordningDto.TilsynPeriodeInfoDto>?,
        )

        data class LovbestemtFerieDto(
            val perioder: List<String>?,
        )

        data class ArbeidstidDto(
            val arbeidstakerList: List<ArbeidAktivitetDto.ArbeidstakerDto>?,
            val frilanserArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
            val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
        )

        data class UttakDto(
            val perioder: Map<String, PleiepengerSøknadVisningDto.PleiepengerYtelseDto.UttakDto.UttakPeriodeInfoDto>,
        )
    }
}
