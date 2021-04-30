package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDate

data class PleiepengerSøknadVisningDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto? = null,
    val journalposter: List<JournalpostIdDto>? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    val barn: BarnDto? = null,
    val sendtInn: Boolean? = false,
    val erFraK9: Boolean? = false,
    val soeknadsperiode: PeriodeDto? = null,
    val opptjeningAktivitet: ArbeidAktivitetDto? = null,
    val arbeidstid: ArbeidstidDto? = null,
    val beredskap: List<BeredskapDto>? = null,
    val nattevaak: List<NattevåkDto>? = null,
    val tilsynsordning: TilsynsordningDto? = null,
    val uttak: List<UttakDto>? = null,
    val omsorg: OmsorgDto? = null,
    val bosteder: List<BostederDto>? = null,
    val lovbestemtFerie: List<PeriodeDto>? = null,
    val soknadsinfo: DataBruktTilUtledningDto? = null,
    val utenlandsopphold : List<UtenlandsoppholdDto>? = null

) {
    data class BarnDto(
        val norskIdent: NorskIdentDto?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?,
    )

    data class ArbeidAktivitetDto(
        val selvstendigNæringsdrivende: List<SelvstendigNæringsdrivendeDto>?,
        val frilanser: FrilanserDto?,
        val arbeidstaker: List<ArbeidstakerDto>?,
    ) {
        data class SelvstendigNæringsdrivendeDto(
            val organisasjonsnummer: String?,
            val virksomhetNavn: String?,
            val perioder: List<SelvstendigNæringsdrivendePeriodeInfoDto>?,
        ) {
            data class SelvstendigNæringsdrivendePeriodeInfoDto(
                val periode: PeriodeDto?,
                val virksomhetstyper: List<String>?,
                val registrertIUtlandet: Boolean?,
                val landkode: String?,
                val regnskapsførerNavn: String?,
                val regnskapsførerTlf: String?,
                val bruttoInntekt: BigDecimal?,
                val erNyoppstartet: Boolean?
            )
        }

        data class FrilanserDto(
            val startdato: String?,
            val jobberFortsattSomFrilans: Boolean?,
        )

        data class ArbeidstakerDto(
            val norskIdent: NorskIdentDto?,
            val organisasjonsnummer: String?,
            val arbeidstidInfo: ArbeidstidInfoDto?,
        ) {
            data class ArbeidstidInfoDto(
                val perioder: List<ArbeidstidPeriodeInfoDto>?,
                val jobberNormaltTimerPerDag: String?,
            ) {
                data class ArbeidstidPeriodeInfoDto(
                    val periode: PeriodeDto?,
                    val faktiskArbeidTimerPerDag: String?,
                )
            }
        }
    }

    data class ArbeidstidDto(
        val arbeidstakerList: List<ArbeidAktivitetDto.ArbeidstakerDto>?,
        val frilanserArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto.ArbeidstidPeriodeInfoDto?,
        val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto?,
    )

    data class DataBruktTilUtledningDto(
        val samtidigHjemme: Boolean? = null,
        val harMedsøker: Boolean? = null,
    )

    data class BostederDto(
        val periode: PeriodeDto?,
        val land: String?,
    )

    data class UtenlandsoppholdDto(
        val periode: PeriodeDto?,
        val land: String?,
        val årsak: String?,
    )

    data class BeredskapDto(
        val periode: PeriodeDto?,
        val tilleggsinformasjon: String?,

        )

    data class NattevåkDto(
        val periode: PeriodeDto?,
        val tilleggsinformasjon: String?,
    )

    data class TilsynsordningDto(
        val perioder: List<TilsynsordningInfoDto>?,
    )

    data class TilsynsordningInfoDto(
        val periode: PeriodeDto?,
        val timer: Int,
        val minutter: Int
    )

    data class UttakDto(
        val periode: PeriodeDto?,
        val timerPleieAvBarnetPerDag: String?,
    )

    data class OmsorgDto(
        val relasjonTilBarnet: String?,
        val samtykketOmsorgForBarnet: Boolean?,
        val beskrivelseAvOmsorgsrollen: String?,
    )
}
