package no.nav.k9punsj.korrigeringinntektsmelding

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.dto.Mappe
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.felles.dto.TimerOgMinutter
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.utils.objectMapper
import java.time.LocalDate
import java.time.LocalTime

data class KorrigeringInntektsmeldingDto(
    val soeknadId: String,
    val soekerId: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime? = null,
    val journalposter: List<String>? = null,
    val organisasjonsnummer: String? = null,
    val arbeidsforholdId: String? = null,
    val fravaersperioder: List<FraværPeriode>? = null,
    val harInfoSomIkkeKanPunsjes: Boolean? = null,
    val harMedisinskeOpplysninger: Boolean? = null,
    val k9saksnummer: String? = null,
) {
    data class FraværPeriode(
        val periode: PeriodeDto,
        val faktiskTidPrDag: String?,
        val tidPrDag: TimerOgMinutter? = faktiskTidPrDag?.somDuration()
            ?.somTimerOgMinutter()?.somTimerOgMinutterDto()
    )
}

data class SvarOmsDto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<KorrigeringInntektsmeldingDto>?
)

internal fun Mappe.tilOmsVisning(norskIdent: String): SvarOmsDto {
    val bunke = hentFor(PunsjFagsakYtelseType.OMSORGSPENGER)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsDto(norskIdent, PunsjFagsakYtelseType.OMSORGSPENGER.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue<KorrigeringInntektsmeldingDto>(s.søknad).copy(
                    k9saksnummer = s.k9saksnummer,)
            } else {
                KorrigeringInntektsmeldingDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s))
            }
        }
    return SvarOmsDto(norskIdent, PunsjFagsakYtelseType.OMSORGSPENGER.kode, søknader)
}

internal fun SøknadEntitet.tilOmsvisning(): KorrigeringInntektsmeldingDto {
    if (søknad == null) {
        return KorrigeringInntektsmeldingDto(soeknadId = this.søknadId, k9saksnummer = k9saksnummer)
    }
    return objectMapper().convertValue<KorrigeringInntektsmeldingDto>(søknad).copy(k9saksnummer = k9saksnummer)
}
