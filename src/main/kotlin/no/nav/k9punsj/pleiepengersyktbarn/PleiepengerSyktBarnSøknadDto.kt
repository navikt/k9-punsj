package no.nav.k9punsj.pleiepengersyktbarn

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.*
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somDuration
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import no.nav.k9punsj.utils.objectMapper
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

data class PleiepengerSyktBarnSøknadDto(
    val soeknadId: String,
    val soekerId: String? = null,
    val journalposter: List<String>? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime? = null,
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
    val utenlandsopphold: List<UtenlandsoppholdDto>? = null,
    val utenlandsoppholdV2: List<UtenlandsoppholdDtoV2> = emptyList(),
    val harInfoSomIkkeKanPunsjes: Boolean,
    val harMedisinskeOpplysninger: Boolean,
    val trekkKravPerioder: Set<PeriodeDto> = emptySet(),
    val begrunnelseForInnsending: BegrunnelseForInnsending? = null,
    val metadata: Map<*, *>? = null,
    val k9saksnummer: String? = null
) {

    data class BarnDto(
        val norskIdent: String?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )

    data class DataBruktTilUtledningDto(
        val samtidigHjemme: Boolean? = null,
        val harMedsoeker: Boolean? = null
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
        val tidsformat: Tidsformat?,
        val timer: Long,
        val minutter: Int,
        val perDagString: String?
    ) {
        fun somDuration(): Duration {
            return TimerOgMinutter(timer, minutter).somDuration()
        }

        companion object {
            @JvmStatic
            @JsonCreator
            fun create(periode: PeriodeDto?,
                       tidsformat: Tidsformat?,
                       timer: Long,
                       minutter: Int,
                       perDagString: String?): TilsynsordningInfoDto =
                when (tidsformat) {
                    Tidsformat.desimaler -> {
                        val duration = perDagString.somDuration()
                        TilsynsordningInfoDto(periode, tidsformat, duration?.toHours() ?: 0, duration?.toMinutesPart() ?: 0, perDagString)
                    }
                    else -> TilsynsordningInfoDto(periode, tidsformat, timer, minutter, null)
                }
            }
    }

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
}

enum class Tidsformat {
    desimaler, timerOgMin
}

data class SvarPsbDto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<PleiepengerSyktBarnSøknadDto>?
)

internal fun Mappe.tilPsbVisning(norskIdent: String): SvarPsbDto {
    val bunke = hentFor(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarPsbDto(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s: SøknadEntitet ->
            if (s.søknad != null) {
                objectMapper().convertValue<PleiepengerSyktBarnSøknadDto>(s.søknad).copy(
                    k9saksnummer = s.k9saksnummer,
                )
            } else {
                PleiepengerSyktBarnSøknadDto(
                    soeknadId = s.søknadId,
                    soekerId = norskIdent,
                    journalposter = hentUtJournalposter(s),
                    harMedisinskeOpplysninger = false,
                    harInfoSomIkkeKanPunsjes = false,
                    k9saksnummer = s.k9saksnummer
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
            harMedisinskeOpplysninger = false,
            k9saksnummer = k9saksnummer
        )
    }
    return objectMapper().convertValue<PleiepengerSyktBarnSøknadDto>(søknad).copy(
        k9saksnummer = this.k9saksnummer
    )
}
