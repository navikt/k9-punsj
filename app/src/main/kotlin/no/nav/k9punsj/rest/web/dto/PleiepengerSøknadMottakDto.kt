package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto.*
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class PleiepengerSøknadMottakDto(

    val søker: SøkerDto?,
    val mottattDato: ZonedDateTime?,
    val ytelse: PleiepengerYtelseDto?,
) {

    data class SøkerDto(
        val norskIdentitetsnummer: NorskIdentDto?,
    )

    data class PleiepengerYtelseDto(
        val barn: BarnDto?,
        val søknadsperiode: String?,
        val opptjeningAktivitet: ArbeidAktivitetDto?,
        val soknadsinfo: DataBruktTilUtledningDto?,
        val bosteder: BostederDto?,
        val utenlandsopphold: UtenlandsoppholdDto?,
        val beredskap: BeredskapDto?,
        val nattevåk: NattevåkDto?,
        val tilsynsordning: TilsynsordningDto?,
        val lovbestemtFerie: LovbestemtFerieDto?,
        val arbeidstid: ArbeidstidDto?,
        val uttak: UttakDto?,
        val omsorg: OmsorgDto?,
        val infoFraPunsj : Boolean?,
    ) {

        data class BarnDto(
            val norskIdentitetsnummer: NorskIdentDto?,
            @JsonFormat(pattern = "yyyy-MM-dd")
            val fødselsdato: LocalDate?,
        )

        data class ArbeidAktivitetDto(
            val selvstendigNæringsdrivende: List<SelvstendigNæringsdrivendeDto>?,
            val frilanser: PleiepengerSøknadVisningDto.ArbeidAktivitetDto.FrilanserDto?,
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
                @JsonFormat(pattern = "yyyy-MM-dd")
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
                    val perioder: Map<String, ArbeidstidPeriodeInfoDto>?,
                ) {
                    data class ArbeidstidPeriodeInfoDto(
                        val jobberNormaltTimerPerDag: Duration?,
                        val faktiskArbeidTimerPerDag: Duration?,
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
                val etablertTilsynTimerPerDag: Duration?,
            )
        }

        data class LovbestemtFerieDto(
            val perioder: Map<String, LovbestemtFerieInfoDto>?,
        )

        data class LovbestemtFerieInfoDto(
            val info: String?
        )

        data class ArbeidstidDto(
            val arbeidstakerList: List<ArbeidAktivitetDto.ArbeidstakerDto>?,
            val frilanserArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
            val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
        )

        data class UttakDto(
            val perioder: Map<String, UttakPeriodeInfoDto>?,
        ) {
            data class UttakPeriodeInfoDto(
                val timerPleieAvBarnetPerDag: String?,
            )
        }
    }
}
