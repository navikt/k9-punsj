package no.nav.k9punsj.korrigeringinntektsmelding

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.Mappe
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.dto.SøknadDto
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import java.time.LocalDate
import java.time.LocalTime

data class KorrigeringInntektsmeldingDto(
    override val soeknadId: String,
    override val soekerId: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    override val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    override val klokkeslett: LocalTime? = null,
    override val journalposter: List<String>? = null,
    val organisasjonsnummer: String? = null,
    val arbeidsforholdId: String? = null,
    val fravaersperioder: List<FraværPeriode>? = null,
    val harInfoSomIkkeKanPunsjes: Boolean? = null,
    val harMedisinskeOpplysninger: Boolean? = null
): SøknadDto {
    data class FraværPeriode(
        val periode: PeriodeDto,
        val faktiskTidPrDag: String?,
        val tidPrDag: PleiepengerSyktBarnSøknadDto.TimerOgMinutter? = faktiskTidPrDag?.somDuration()
            ?.somTimerOgMinutter()?.somTimerOgMinutterDto(),
    )
}

data class SvarOmsDto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<KorrigeringInntektsmeldingDto>?,
)

internal fun Mappe.tilOmsVisning(norskIdent: String): SvarOmsDto {
    val bunke = hentFor(FagsakYtelseType.OMSORGSPENGER)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsDto(norskIdent, FagsakYtelseType.OMSORGSPENGER.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                KorrigeringInntektsmeldingDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s))
            }
        }
    return SvarOmsDto(norskIdent, FagsakYtelseType.OMSORGSPENGER.kode, søknader)
}

internal fun SøknadEntitet.tilOmsvisning(): KorrigeringInntektsmeldingDto {
    if (søknad == null) {
        return KorrigeringInntektsmeldingDto(soeknadId = this.søknadId)
    }
    return objectMapper().convertValue(søknad)
}
