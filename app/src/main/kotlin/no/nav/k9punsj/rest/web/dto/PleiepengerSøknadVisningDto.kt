package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.math.BigDecimal
import java.time.LocalDate

data class PleiepengerSøknadVisningDto(

    val søker: SøkerDto?,
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
        data class BarnDto(
            val norskIdentitetsnummer: NorskIdentDto?,
            val fødselsdato: LocalDate?,
        )

        data class ArbeidAktivitetDto(
            val selvstendigNæringsdrivende: List<SelvstendigNæringsdrivendeDto>?,
            val frilanser: FrilanserDto?,
            val arbeidstaker: List<ArbeidstakerDto>?,
        ) {
            data class SelvstendigNæringsdrivendeDto(
                @JsonSerialize(keyUsing = MyKeySerializer::class)
                @JsonDeserialize(keyUsing = MyKeyDeserializer::class)
                val perioder: Map<PeriodeDto, SelvstendigNæringsdrivendePeriodeInfoDto>?,
                val organisasjonsnummer: String?,
                val virksomhetNavn: String?,
            ) {
                data class SelvstendigNæringsdrivendePeriodeInfoDto(
                    val virksomhetstyper: List<String>?,
                    val regnskapsførerNavn: String?,
                    val regnskapsførerTlf: String?,
                    val erVarigEndring: Boolean?,
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
                    @JsonSerialize(keyUsing = MyKeySerializer::class)
                    @JsonDeserialize(keyUsing = MyKeyDeserializer::class)
                    val perioder: Map<PeriodeDto, ArbeidstidPeriodeInfoDto>?,
                ) {
                    data class ArbeidstidPeriodeInfoDto(
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
            @JsonSerialize(keyUsing = MyKeySerializer::class)
            @JsonDeserialize(keyUsing = MyKeyDeserializer::class)
            val perioder: Map<PeriodeDto, BostedPeriodeInfoDto>?,
        ) {
            data class BostedPeriodeInfoDto(
                val land: String?,
            )
        }

        data class UtenlandsoppholdDto(
            @JsonSerialize(keyUsing = MyKeySerializer::class)
            @JsonDeserialize(keyUsing = MyKeyDeserializer::class)
            val perioder: Map<PeriodeDto, UtenlandsoppholdPeriodeInfoDto>?,

            ) {
            data class UtenlandsoppholdPeriodeInfoDto(
                val land: String?,
                val årsak: String?,
            )
        }

        data class BeredskapDto(
            @JsonSerialize(keyUsing = MyKeySerializer::class)
            @JsonDeserialize(keyUsing = MyKeyDeserializer::class)
            val perioder: Map<PeriodeDto, BeredskapPeriodeInfoDto>?,

            ) {
            data class BeredskapPeriodeInfoDto(
                val tilleggsinformasjon: String?,
            )
        }

        data class NattevåkDto(
            @JsonSerialize(keyUsing = MyKeySerializer::class)
            @JsonDeserialize(keyUsing = MyKeyDeserializer::class)
            val perioder: Map<PeriodeDto, NattevåkPeriodeInfoDto>?,
        ) {
            data class NattevåkPeriodeInfoDto(
                val tilleggsinformasjon: String?,
            )
        }

        data class TilsynsordningDto(
            @JsonSerialize(keyUsing = MyKeySerializer::class)
            @JsonDeserialize(keyUsing = MyKeyDeserializer::class)
            val perioder: Map<PeriodeDto, TilsynPeriodeInfoDto>?,
        ) {
            data class TilsynPeriodeInfoDto(
                val etablertTilsynTimerPerDag: String?,
            )
        }

        data class LovbestemtFerieDto(
            val perioder: List<PeriodeDto>?,
        )

        data class ArbeidstidDto(
            val arbeidstakerList: List<ArbeidAktivitetDto.ArbeidstakerDto>?,
            val frilanserArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
            val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
        )

        data class UttakDto(
            @JsonSerialize(keyUsing = MyKeySerializer::class)
            @JsonDeserialize(keyUsing = MyKeyDeserializer::class)
            val perioder: Map<PeriodeDto, UttakPeriodeInfoDto>?,
        ) {
            data class UttakPeriodeInfoDto(
                val timerPleieAvBarnetPerDag: String?,
            )
        }

        data class OmsorgDto(
            val relasjonTilBarnet: String?,
            val samtykketOmsorgForBarnet: Boolean?,
            val beskrivelseAvOmsorgsrollen: String?
        )
    }
}
