package no.nav.k9punsj.omsorgspengerutbetaling

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.felles.fravær.AktivitetFravær
import no.nav.k9.søknad.felles.fravær.FraværÅrsak
import no.nav.k9.søknad.felles.fravær.SøknadÅrsak
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidAktivitetDto
import no.nav.k9punsj.felles.dto.BostederDto
import no.nav.k9punsj.felles.dto.Mappe
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SøknadEntitet
import no.nav.k9punsj.felles.dto.TimerOgMinutter
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import no.nav.k9punsj.felles.dto.UtenlandsoppholdDto
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.utils.objectMapper
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
    val bosteder: List<BostederDto>? = null,
    val utenlandsopphold: List<UtenlandsoppholdDto> = emptyList(),
    val opptjeningAktivitet: ArbeidAktivitetDto? = null,
    val fravaersperioder: List<FraværPeriode>? = null,
    val harInfoSomIkkeKanPunsjes: Boolean? = null,
    val harMedisinskeOpplysninger: Boolean? = null,
    val erKorrigering: Boolean ? = null,
    val metadata: Map<*, *>? = null,
    val k9saksnummer: String? = null
) {
    data class FraværPeriode(
        val aktivitetsFravær: AktivitetFravær,
        val organisasjonsnummer: String? = null,
        val periode: PeriodeDto,
        val fraværÅrsak: FraværÅrsak?,
        val søknadÅrsak: SøknadÅrsak?,
        val faktiskTidPrDag: String?,
        val tidPrDag: TimerOgMinutter? = faktiskTidPrDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto(),
        val normalArbeidstidPrDag: String?,
        val normalArbeidstid: TimerOgMinutter? = normalArbeidstidPrDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto()
    )

    data class BarnDto(
        val norskIdent: String?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foedselsdato: LocalDate?
    )

    /*
     * Plukker ut første fraværsperiode og skaper ny Periode for det året.
     */
    fun periodeForHeleAretMedFravaer(): PeriodeDto? {
        return if(fravaersperioder.isNullOrEmpty() || fravaersperioder.first().periode.fom == null) {
            null
        } else {
            val aar = fravaersperioder.first().periode.fom!!.year
            PeriodeDto(
                fom = LocalDate.of(aar, 1, 1),
                tom = LocalDate.of(aar, 12, 31)
            )
        }
    }
}

data class SvarOmsUtDto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerutbetalingSøknadDto>?
)

internal fun Mappe.tilOmsUtVisning(norskIdent: String): SvarOmsUtDto {
    val bunke = hentFor(FagsakYtelseType.OMSORGSPENGER)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsUtDto(norskIdent, FagsakYtelseType.OMSORGSPENGER.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue<OmsorgspengerutbetalingSøknadDto>(s.søknad).copy(
                    k9saksnummer = s.k9saksnummer
                )
            } else {
                OmsorgspengerutbetalingSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s), k9saksnummer = s.k9saksnummer)
            }
        }
    return SvarOmsUtDto(norskIdent, FagsakYtelseType.OMSORGSPENGER.kode, søknader)
}

internal fun SøknadEntitet.tilOmsUtvisning(): OmsorgspengerutbetalingSøknadDto {
    if (søknad == null) {
        return OmsorgspengerutbetalingSøknadDto(soeknadId = this.søknadId, k9saksnummer = k9saksnummer)
    }
    return objectMapper().convertValue<OmsorgspengerutbetalingSøknadDto>(søknad).copy(
        k9saksnummer = k9saksnummer
    )
}
