package no.nav.k9punsj.omsorgspengerutbetaling

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.dto.*
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.omsorgspengermidlertidigalene.OmsorgspengerMidlertidigAleneSøknadDto
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto
import java.time.LocalDate
import java.time.LocalTime

data class OmsorgspengerutbetalingSøknadDto(
    val soeknadId: String,
    val soekerId: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime? = null,
    val barn: List<BarnDto> = emptyList(),
    val journalposter: List<String>? = null,
    val opptjeningAktivitet: ArbeidAktivitetDto? = null,
    val organisasjonsnummer: String? = null,
    val arbeidsforholdId: String? = null,
    val fravaersperioder: List<FraværPeriode>? = null,
    val harInfoSomIkkeKanPunsjes: Boolean? = null,
    val harMedisinskeOpplysninger: Boolean? = null
) {
    data class FraværPeriode(
        val periode: PeriodeDto,
        val faktiskTidPrDag: String?,
        val tidPrDag: TimerOgMinutter? = faktiskTidPrDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto(),
    )

    data class BarnDto(
        val norskIdent: String?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )
}

data class SvarOmsUtDto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerutbetalingSøknadDto>?,
)

internal fun Mappe.tilOmsUtVisning(norskIdent: String): SvarOmsUtDto {
    val bunke = hentFor(FagsakYtelseType.OMSORGSPENGERUTBETALING)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsUtDto(norskIdent, FagsakYtelseType.OMSORGSPENGERUTBETALING.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                OmsorgspengerutbetalingSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s))
            }
        }
    return SvarOmsUtDto(norskIdent, FagsakYtelseType.OMSORGSPENGERUTBETALING.kode, søknader)
}

internal fun SøknadEntitet.tilOmsUtvisning(): OmsorgspengerutbetalingSøknadDto {
    if (søknad == null) {
        return OmsorgspengerutbetalingSøknadDto(soeknadId = this.søknadId)
    }
    return objectMapper().convertValue(søknad)
}
