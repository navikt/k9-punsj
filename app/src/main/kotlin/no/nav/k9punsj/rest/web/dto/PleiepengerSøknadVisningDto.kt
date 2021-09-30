package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somDuration
import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class PleiepengerSøknadVisningDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto? = null,
    val journalposter: List<JournalpostIdDto>? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett : LocalTime? = null,
    val barn: BarnDto? = null,
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
    val lovbestemtFerieSomSkalSlettes: List<PeriodeDto>? = null,
    val soknadsinfo: DataBruktTilUtledningDto? = null,
    val utenlandsopphold : List<UtenlandsoppholdDto>? = null,
    val harInfoSomIkkeKanPunsjes : Boolean,
    val harMedisinskeOpplysninger : Boolean,
    val trekkKravPerioder: Set<PeriodeDto> = emptySet()) {
    data class BarnDto(
        val norskIdent: NorskIdentDto?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )

    data class ArbeidAktivitetDto(
        val selvstendigNaeringsdrivende: SelvstendigNæringsdrivendeDto?,
        val frilanser: FrilanserDto?,
        val arbeidstaker: List<ArbeidstakerDto>?) {
        data class SelvstendigNæringsdrivendeDto(
            val organisasjonsnummer: String?,
            val virksomhetNavn: String?,
            val info: SelvstendigNæringsdrivendePeriodeInfoDto?) {
            data class SelvstendigNæringsdrivendePeriodeInfoDto(
                val periode: PeriodeDto?,
                val virksomhetstyper: List<String>?,
                val registrertIUtlandet: Boolean?,
                val landkode: String?,
                val regnskapsførerNavn: String?,
                val regnskapsførerTlf: String?,
                val bruttoInntekt: BigDecimal?,
                val erNyoppstartet: Boolean?,
                val erVarigEndring: Boolean?,
                val endringInntekt: BigDecimal?,
                @JsonFormat(pattern = "yyyy-MM-dd")
                val endringDato: LocalDate?,
                val endringBegrunnelse: String?
            )
        }

        data class FrilanserDto(
            @JsonFormat(pattern = "yyyy-MM-dd")
            val startdato: String?,
            @JsonFormat(pattern = "yyyy-MM-dd")
            val sluttdato: String?,
            val jobberFortsattSomFrilans: Boolean?
        )

        data class ArbeidstakerDto(
            val norskIdent: NorskIdentDto?,
            val organisasjonsnummer: String?,
            val arbeidstidInfo: ArbeidstidInfoDto?) {
            data class ArbeidstidInfoDto(
                val perioder: List<ArbeidstidPeriodeInfoDto>?) {
                data class ArbeidstidPeriodeInfoDto(
                    val periode: PeriodeDto?,
                    val faktiskArbeidTimerPerDag: String?,
                    val jobberNormaltTimerPerDag: String?,
                    val faktiskArbeidPerDag: TimerOgMinutter? = faktiskArbeidTimerPerDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto(),
                    val jobberNormaltPerDag: TimerOgMinutter? = jobberNormaltTimerPerDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto()
                )
            }
        }
    }

    data class ArbeidstidDto(
        val arbeidstakerList: List<ArbeidAktivitetDto.ArbeidstakerDto>?,
        val frilanserArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?,
        val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitetDto.ArbeidstakerDto.ArbeidstidInfoDto?
    )

    data class DataBruktTilUtledningDto(
        val samtidigHjemme: Boolean? = null,
        val harMedsoeker: Boolean? = null
    )

    data class BostederDto(
        val periode: PeriodeDto?,
        val land: String?
    )

    data class UtenlandsoppholdDto(
        val periode: PeriodeDto?,
        val land: String?,
        val årsak: String?
    )

    data class BeredskapDto(
        val periode: PeriodeDto?,
        val tilleggsinformasjon: String?

        )

    data class NattevåkDto(
        val periode: PeriodeDto?,
        val tilleggsinformasjon: String?
    )

    data class TilsynsordningDto(
        val perioder: List<TilsynsordningInfoDto>?
    )

    data class TilsynsordningInfoDto(
        val periode: PeriodeDto?,
        val timer: Int,
        val minutter: Int
    )

    data class UttakDto(
        val periode: PeriodeDto?,
        val timerPleieAvBarnetPerDag: String?,
        val pleieAvBarnetPerDag: TimerOgMinutter? = timerPleieAvBarnetPerDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto()
    )

    data class OmsorgDto(
        val relasjonTilBarnet: String?,
        val samtykketOmsorgForBarnet: Boolean?,
        val beskrivelseAvOmsorgsrollen: String?
    )

    data class TimerOgMinutter(
        val timer: Long,
        val minutter: Int) {
        internal companion object {
            internal fun Pair<Long,Int>?.somTimerOgMinutterDto() = when (this) {
                null -> null
                else -> TimerOgMinutter(first, second)
            }
        }
    }
}
