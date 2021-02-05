package no.nav.k9punsj.rest.web.dto

import java.math.BigDecimal
import java.time.LocalDate

data class PleiepengerSøknadDto(

    val søker: SøkerDto?,
    val ytelse: PleiepengerYtelseDto?,
) {
    data class SøkerDto(
        val norskIdentitetsnummer: NorskIdentDto?,
    )

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
        val arbeid: ArbeidDto?,
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
                val perioder: Map<String, SelvstendigNæringsdrivendePeriodeInfoDto>?,
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
                val jobberFortsattSomFrilans: Boolean?

                )

            data class ArbeidstakerDto(val ikkeFerdig: String?)
        }

        data class DataBruktTilUtledningDto(
            val harForståttRettigheterOgPlikter: Boolean?,
            val harBekreftetOpplysninger: Boolean?,
            val samtidigHjemme: Boolean?,
            val harMedsøker: Boolean?,
            val bekrefterPeriodeOver8Uker: Boolean?,
        )

        data class BostederDto(
            val perioder: Map<String, BostedPeriodeInfoDto>?,
        ) {
            data class BostedPeriodeInfoDto(
                val land: String?,
            )
        }

        data class UtenlandsoppholdDto(val ikkeFerdig: String?)
        data class BeredskapDto(val ikkeFerdig: String?)
        data class NattevåkDto(val ikkeFerdig: String?)
        data class TilsynsordningDto(val ikkeFerdig: String?)
        data class LovbestemtFerieDto(val ikkeFerdig: String?)
        data class ArbeidDto(val ikkeFerdig: String?)
        data class UttakDto(val ikkeFerdig: String?)
        data class OmsorgDto(val ikkeFerdig: String?)
    }
}
