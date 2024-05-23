package no.nav.k9punsj.pleiepengerlivetssluttfase

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.ArbeidstidDto
import no.nav.k9punsj.felles.dto.BostederDto
import no.nav.k9punsj.felles.dto.Mappe
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.PleietrengendeDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.felles.dto.TimerOgMinutter
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import no.nav.k9punsj.felles.dto.UtenlandsoppholdDto
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.utils.objectMapper
import java.time.LocalDate
import java.time.LocalTime

data class PleiepengerLivetsSluttfaseSøknadDto(
    val soeknadId: String,
    val soekerId: String? = null,
    val journalposter: List<String>? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime? = null,
    val soeknadsperiode: List<PeriodeDto>? = null,
    val pleietrengende: PleietrengendeDto? = null,
    val arbeidstid: ArbeidstidDto? = null,
    val uttak: List<UttakDto>? = null,
    val opptjeningAktivitet: ArbeidAktivitetDto? = null,
    val bosteder: List<BostederDto>? = null,
    val lovbestemtFerie: List<PeriodeDto>? = null,
    val lovbestemtFerieSomSkalSlettes: List<PeriodeDto>? = null,
    val utenlandsopphold: List<UtenlandsoppholdDto>? = null,
    val harInfoSomIkkeKanPunsjes: Boolean,
    val harMedisinskeOpplysninger: Boolean,
    val trekkKravPerioder: Set<PeriodeDto> = emptySet(),
    val begrunnelseForInnsending: BegrunnelseForInnsending? = null,
    val metadata: Map<*, *>? = null
) {

    data class UttakDto(
        val periode: PeriodeDto?,
        val timerPleieAvPleietrengendePerDag: String?,
        val pleieAvPleietrengendePerDag: TimerOgMinutter? = timerPleieAvPleietrengendePerDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto()
    )
}

data class SvarPlsDto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<PleiepengerLivetsSluttfaseSøknadDto>?
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
