package no.nav.k9punsj.opplaeringspenger

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.felles.type.BegrunnelseForInnsending
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.*
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import no.nav.k9punsj.felles.dto.hentUtJournalposter
import no.nav.k9punsj.objectMapper
import java.time.LocalDate
import java.time.LocalTime

data class OpplaeringspengerSøknadDto(
    val soeknadId: String,
    val soekerId: String? = null,
    val journalposter: List<String>? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattDato: LocalDate? = null,
    @JsonFormat(pattern = "HH:mm")
    val klokkeslett: LocalTime? = null,
    val barn: BarnDto? = null,
    val soeknadsperiode: List<PeriodeDto>? = null,
    val trekkKravPerioder: Set<PeriodeDto> = emptySet(),
    val opptjeningAktivitet: ArbeidAktivitetDto? = null,
    val soknadsinfo: DataBruktTilUtledningDto? = null,
    val bosteder: List<BostederDto>? = null,
    val utenlandsopphold: List<UtenlandsoppholdDto> = emptyList(),
    val lovbestemtFerie: List<PeriodeDto>? = null,
    val arbeidstid: ArbeidstidDto? = null,
    val uttak: List<UttakDto>? = null,
    val omsorg: OmsorgDto? = null,
    val kurs: Kurs? = null,
    val harInfoSomIkkeKanPunsjes: Boolean,
    val harMedisinskeOpplysninger: Boolean,
    val begrunnelseForInnsending: BegrunnelseForInnsending? = null,
    val metadata: Map<*, *>? = null
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

    data class Kurs(
        val kursHolder: KursHolder?,
        val kursperioder: List<KursPeriodeMedReisetid>?
    )

    data class KursPeriodeMedReisetid(
        val periode: PeriodeDto?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val avreise: LocalDate?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val hjemkomst: LocalDate?
    )

    data class KursHolder(
        val holder: String?,
        val institusjonsUuid: String?
    )
}

data class SvarOlpDto(
    val søker: String,
    val fagsakTypeKode: String,
    val søknader: List<OpplaeringspengerSøknadDto>?
)

internal fun Mappe.tilOlpVisning(norskIdent: String): SvarOlpDto {
    val bunke = hentFor(FagsakYtelseType.OPPLÆRINGSPENGER)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOlpDto(norskIdent, FagsakYtelseType.OPPLÆRINGSPENGER.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                OpplaeringspengerSøknadDto(
                    soeknadId = s.søknadId,
                    soekerId = norskIdent,
                    journalposter = hentUtJournalposter(s),
                    harMedisinskeOpplysninger = false,
                    harInfoSomIkkeKanPunsjes = false
                )
            }
        }
    return SvarOlpDto(norskIdent, FagsakYtelseType.OPPLÆRINGSPENGER.kode, søknader)
}

internal fun SøknadEntitet.tilOlpvisning(): OpplaeringspengerSøknadDto {
    if (søknad == null) {
        return OpplaeringspengerSøknadDto(
            soeknadId = this.søknadId,
            journalposter = hentUtJournalposter(this),
            harInfoSomIkkeKanPunsjes = false,
            harMedisinskeOpplysninger = false
        )
    }
    return objectMapper().convertValue(søknad)
}
