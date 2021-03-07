package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime

data class PleiepengerSøknadVisningDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    val mottattDato: ZonedDateTime?,
    val barn: BarnDto?,
    val journalposter: List<JournalpostIdDto>,
    val sendtInn: Boolean,
    val erFraK9: Boolean,
    val soeknadsperiode: PeriodeDto,
    val arbeidAktivitet: ArbeidAktivitetDto?,
    val arbeidstid: ArbeidstidDto?,
    val beredskap: List<BeredskapDto>,
    val nattevaak: List<NattevåkDto>,
    val tilsynsordning: List<TilsynsordningDto>,
    val uttak: List<UttakDto>,
    val omsorg: OmsorgDto,
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
            val norskIdent: NorskIdentDto?,
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

    data class ArbeidstidDto(
        val arbeidstakerList: List<ArbeidAktivitetDto.ArbeidstakerDto>?,
        val frilanserArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
        val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
    )

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
