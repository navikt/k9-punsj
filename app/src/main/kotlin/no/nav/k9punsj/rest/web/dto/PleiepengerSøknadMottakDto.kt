package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto.*
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto.PleiepengerYtelseDto.*
import java.math.BigDecimal
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
                val perioder: Map<String, SelvstendigNæringsdrivendePeriodeInfoDto>?,
                val organisasjonsnummer: String?,
                val virksomhetNavn: String?,
            )

            data class SelvstendigNæringsdrivendePeriodeInfoDto(
                val virksomhetstyper: List<String>?,
                val regnskapsførerNavn: String?,
                val regnskapsførerTlf: String?,
                val erVarigEndring: Boolean?,
                @JsonFormat(pattern = "yyyy-MM-dd")
                val endringDato: LocalDate?,
                val endringBegrunnelse: String?,
                val bruttoInntekt: BigDecimal?,
                val erNyoppstartet: Boolean?,
                val registrertIUtlandet: Boolean?,
                val landkode: String?,
            )

            data class ArbeidstakerDto(
                val norskIdentitetsnummer: NorskIdentDto?,
                val organisasjonsnummer: String?,
                val arbeidstidInfo: ArbeidstidInfoDto,
            ) {
                data class ArbeidstidInfoDto(
                    val jobberNormaltTimerPerDag: String?,
                    val perioder: Map<String, ArbeidstidPeriodeInfoDto>?,
                ) {
                    data class ArbeidstidPeriodeInfoDto(
                        val faktiskArbeidTimerPerDag: String?,
                    )
                }
            }
        }

        data class BostederDto(
            val perioder: Map<String, BostedPeriodeInfoDto>?,
        ) {
            data class BostedPeriodeInfoDto(
                val land: String?,
            )
        }


        data class UtenlandsoppholdDto(
            val perioder: Map<String, UtenlandsoppholdPeriodeInfoDto>?,

            ) {
            data class UtenlandsoppholdPeriodeInfoDto(
                val land: String?,
                val årsak: String?,
            )
        }


        data class BeredskapDto(
            val perioder: Map<String, BeredskapPeriodeInfoDto>?,

            ) {
            data class BeredskapPeriodeInfoDto(
                val tilleggsinformasjon: String?,
            )
        }

        data class NattevåkDto(
            val perioder: Map<String, NattevåkPeriodeInfoDto>?,
        ) {
            data class NattevåkPeriodeInfoDto(
                val tilleggsinformasjon: String?,
            )
        }

        data class TilsynsordningDto(
            val perioder: Map<String, TilsynPeriodeInfoDto>?,
        ) {
            data class TilsynPeriodeInfoDto(
                val etablertTilsynTimerPerDag: String?,
            )
        }

        data class LovbestemtFerieDto(
            val perioder: List<String>?,
        )

        data class ArbeidstidDto(
            val arbeidstakerList: List<ArbeidAktivitetDto.ArbeidstakerDto>?,
            val frilanserArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
            val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
        )

        data class UttakDto(
            val perioder: Map<String, UttakPeriodeInfoDto>,
        ) {
            data class UttakPeriodeInfoDto(
                val timerPleieAvBarnetPerDag: String?,
            )
        }
    }
}
