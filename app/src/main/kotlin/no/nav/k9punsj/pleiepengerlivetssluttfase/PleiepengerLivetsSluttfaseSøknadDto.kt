package no.nav.k9punsj.pleiepengerlivetssluttfase

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.Mappe
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.pleiepengerlivetssluttfase.PleiepengerLivetsSluttfaseSøknadDto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class PleiepengerLivetsSluttfaseSøknadDto(
    val soeknadId: String,
    val soekerId: String? = null,
    val journalposter: List<String>? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett : LocalTime? = null,
    val soeknadsperiode: List<PeriodeDto>? = null,
    val pleietrengende: PleietrengendeDto? = null,
    val arbeidstid: ArbeidstidDto? = null,
    val uttak: List<UttakDto>? = null,
    val opptjeningAktivitet: ArbeidAktivitetDto? = null,
    val bosteder: List<BostederDto>? = null,
    val utenlandsopphold : List<UtenlandsoppholdDto>? = null,
    val harInfoSomIkkeKanPunsjes : Boolean,
    val harMedisinskeOpplysninger : Boolean,
    val trekkKravPerioder: Set<PeriodeDto> = emptySet(),
    val begrunnelseForInnsending: BegrunnelseForInnsending? = null) {

    data class PleietrengendeDto(
        val norskIdent: String? = null,
        val foedselsdato: LocalDate? = null
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
            val norskIdent: String?,
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

    data class BostederDto(
        val periode: PeriodeDto?,
        val land: String?
    )

    data class UtenlandsoppholdDto(
        val periode: PeriodeDto?,
        val land: String?,
        val årsak: String?
    )

    data class UttakDto(
        val periode: PeriodeDto?,
        val timerPleieAvPleietrengendePerDag: String?,
        val pleieAvPleietrengendePerDag: TimerOgMinutter? = timerPleieAvPleietrengendePerDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto()
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

data class SvarPlsDto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<PleiepengerLivetsSluttfaseSøknadDto>?,
)

internal fun Mappe.tilPlsVisning(norskIdent: String): SvarPlsDto {
    val bunke = hentFor(FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarPlsDto(norskIdent, FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                PleiepengerLivetsSluttfaseSøknadDto(
                    soeknadId = s.søknadId,
                    soekerId = norskIdent,
                    journalposter = hentUtJournalposter(s),
                    harMedisinskeOpplysninger = false,
                    harInfoSomIkkeKanPunsjes = false
                )
            }
        }
    return SvarPlsDto(norskIdent, FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE.kode, søknader)
}

internal fun SøknadEntitet.tilPlsvisning(): PleiepengerLivetsSluttfaseSøknadDto {
    if (søknad == null) {
        return PleiepengerLivetsSluttfaseSøknadDto(
            soeknadId = this.søknadId,
            journalposter = hentUtJournalposter(this),
            harInfoSomIkkeKanPunsjes = false,
            harMedisinskeOpplysninger = false
        )
    }
    return objectMapper().convertValue(søknad)
}
