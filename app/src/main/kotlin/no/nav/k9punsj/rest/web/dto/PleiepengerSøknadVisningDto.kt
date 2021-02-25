package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDate

data class PleiepengerSøknadVisningDto(

    val søker: SøkerDto?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate?,
    val ytelse: PleiepengerYtelseDto?,
) {
    data class SøkerDto(
        val norskIdentitetsnummer: NorskIdentDto?,
    )

    data class PleiepengerYtelseDto(
        val barn: BarnDto?,
        val søknadsperiode: PeriodeDto?,
        val arbeidAktivitet: ArbeidAktivitetDto?,
        val dataBruktTilUtledning: DataBruktTilUtledningDto?,
        val bosteder: List<BostederDto>?,
        val utenlandsopphold: List<UtenlandsoppholdDto>?,
        val beredskap: List<BeredskapDto>?,
        val nattevåk: List<NattevåkDto>?,
        val tilsynsordning: List<TilsynsordningDto>?,
        val lovbestemtFerie: LovbestemtFerieDto?,
        val arbeidstid: ArbeidstidDto?,
        val uttak: List<UttakDto>?,
        val omsorg: OmsorgDto?,
    ) {
        data class BarnDto(
            val norskIdentitetsnummer: NorskIdentDto?,
            @JsonFormat(pattern = "yyyy-MM-dd")
            val fødselsdato: LocalDate?,
        )

        data class ArbeidAktivitetDto(
            val selvstendigNæringsdrivende: List<SelvstendigNæringsdrivendeDto>?,
            val frilanser: FrilanserDto?,
            val arbeidstaker: List<ArbeidstakerDto>?,
        ) {
            data class SelvstendigNæringsdrivendeDto(
                val perioder: List<SelvstendigNæringsdrivendePeriodeInfoDto>?,
                val organisasjonsnummer: String?,
                val virksomhetNavn: String?,
            ) {
                data class SelvstendigNæringsdrivendePeriodeInfoDto(
                    val periode: PeriodeDto,
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
            }

            data class FrilanserDto(
                val startdato: String?,
                val jobberFortsattSomFrilans: Boolean?,

                )

            data class ArbeidstakerDto(
                val norskIdentitetsnummer: NorskIdentDto?,
                val organisasjonsnummer: String?,
                val arbeidstidInfo: ArbeidstidInfoDto,
            ) {
                data class ArbeidstidInfoDto(
                    val jobberNormaltTimerPerDag: String?,
                    val perioder: List<ArbeidstidPeriodeInfoDto>?,
                ) {
                    data class ArbeidstidPeriodeInfoDto(
                        val periode: PeriodeDto,
                        val faktiskArbeidTimerPerDag: String?,
                    )
                }
            }
        }

        data class DataBruktTilUtledningDto(
            val harForståttRettigheterOgPlikter: Boolean?,
            val harBekreftetOpplysninger: Boolean?,
            val samtidigHjemme: Boolean?,
            val harMedsøker: Boolean?,
            val bekrefterPeriodeOver8Uker: Boolean?,
        )

        data class BostederDto(
            val periode: PeriodeDto,
            val land: String?,
        )

        data class UtenlandsoppholdDto(
            val periode: PeriodeDto,
            val land: String?,
            val årsak: String?,

            )

        data class BeredskapDto(
            val periode: PeriodeDto,
            val tilleggsinformasjon: String?,

            )

        data class NattevåkDto(
            val periode: PeriodeDto,
            val tilleggsinformasjon: String?,
        )

        data class TilsynsordningDto(
            val periode: PeriodeDto,
            val etablertTilsynTimerPerDag: String?,
        )

        data class LovbestemtFerieDto(
            val perioder: List<PeriodeDto>?,
        )

        data class ArbeidstidDto(
            val arbeidstakerList: List<ArbeidAktivitetDto.ArbeidstakerDto>?,
            val frilanserArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
            val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
        )

        data class UttakDto(
            val periode: PeriodeDto,
            val timerPleieAvBarnetPerDag: String?,
        )

        data class OmsorgDto(
            val relasjonTilBarnet: String?,
            val samtykketOmsorgForBarnet: Boolean?,
            val beskrivelseAvOmsorgsrollen: String?,
        )
    }
}
