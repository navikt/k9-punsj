package no.nav.k9punsj.pleiepengersyktbarn

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.Mappe
import no.nav.k9punsj.db.datamodell.SøknadEntitet
import no.nav.k9punsj.domenetjenester.dto.*
import no.nav.k9punsj.domenetjenester.dto.hentUtJournalposter
import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somDuration
import no.nav.k9punsj.domenetjenester.mappers.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class PleiepengerSyktBarnSøknadDto(
    val soeknadId: SøknadIdDto,
    val soekerId: NorskIdentDto? = null,
    val journalposter: List<JournalpostIdDto>? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett : LocalTime? = null,
    val barn: BarnDto? = null,
    val soeknadsperiode: List<PeriodeDto>? = null,
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
    val utenlandsoppholdV2 : List<UtenlandsoppholdDtoV2> = emptyList(),
    val harInfoSomIkkeKanPunsjes : Boolean,
    val harMedisinskeOpplysninger : Boolean,
    val trekkKravPerioder: Set<PeriodeDto> = emptySet(),
    val begrunnelseForInnsending: BegrunnelseForInnsending? = null) {

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

    data class UtenlandsoppholdDtoV2(
        val periode: PeriodeDto? = null,
        val land: String? = null,
        val innleggelsesperioder : List<InnleggelsesperiodeDto> = emptyList()

    ) {
        data class InnleggelsesperiodeDto(
            val årsak: String?,
            val periode: PeriodeDto?
        )
    }

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

data class SvarPsbDto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<PleiepengerSyktBarnSøknadDto>?,
)

internal fun Mappe.tilPsbVisning(norskIdent: NorskIdentDto): SvarPsbDto {
    val bunke = hentFor(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarPsbDto(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                PleiepengerSyktBarnSøknadDto(
                    soeknadId = s.søknadId,
                    soekerId = norskIdent,
                    journalposter = hentUtJournalposter(s),
                    harMedisinskeOpplysninger = false,
                    harInfoSomIkkeKanPunsjes = false
                )
            }
        }
    return SvarPsbDto(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, søknader)
}

internal fun SøknadEntitet.tilPsbvisning(): PleiepengerSyktBarnSøknadDto {
    if (søknad == null) {
        return PleiepengerSyktBarnSøknadDto(
            soeknadId = this.søknadId,
            journalposter = hentUtJournalposter(this),
            harInfoSomIkkeKanPunsjes = false,
            harMedisinskeOpplysninger = false
        )
    }
    return objectMapper().convertValue(søknad)
}
