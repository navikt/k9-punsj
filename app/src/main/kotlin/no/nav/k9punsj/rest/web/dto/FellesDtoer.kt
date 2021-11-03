package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.Mappe
import no.nav.k9punsj.db.datamodell.SøknadEntitet
import no.nav.k9punsj.objectMapper
import java.time.LocalDate


typealias NorskIdentDto = String
typealias MappeIdDto = String
typealias AktørIdDto = String
typealias BunkeIdDto = String
typealias SøknadIdDto = String
typealias JournalpostIdDto = String

data class PdlPersonDto(
    val norskIdent: NorskIdentDto,
    val aktørId: AktørIdDto,
)

data class BunkeDto<T>(
    val bunkeId: BunkeIdDto,
    val fagsakKode: String,
    val søknader: List<SøknadDto<T>>?,
)

data class SvarPsbDto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<PleiepengerSøknadDto>?,
)

data class SvarOmsDto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerSøknadDto>?,
)

data class PerioderDto(
    val periodeDto: List<PeriodeDto>
)

data class SøknadDto<T>(
    val søknadId: SøknadIdDto,
    val søkerId: NorskIdentDto,
    val barnId: NorskIdentDto? = null,
    val barnFødselsdato: LocalDate? = null,
    val journalposter: JournalposterDto? = null,
    val sendtInn: Boolean? = false,
    val erFraK9: Boolean? = false,
    val søknad: T? = null,
)

data class JournalposterDto(
    val journalposter: MutableSet<String>,
)

data class IdentDto(
    val norskIdent: NorskIdentDto
)

internal fun Mappe.tilPsbVisning(norskIdent: NorskIdentDto): SvarPsbDto {
    val bunke = this.bunke.firstOrNull { b -> b.fagsakYtelseType == FagsakYtelseType.PLEIEPENGER_SYKT_BARN }
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarPsbDto(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
        if (s.søknad != null) {
            objectMapper().convertValue(s.søknad)
        } else {
            PleiepengerSøknadDto(soeknadId = s.søknadId, soekerId = norskIdent, journalposter = hentUtJournalposter(s), harMedisinskeOpplysninger = false, harInfoSomIkkeKanPunsjes = false)
        }
    }
    return SvarPsbDto(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, søknader)
}

internal fun Mappe.tilOmsVisning(norskIdent: NorskIdentDto): SvarOmsDto {
    val bunke = this.bunke.firstOrNull { b -> b.fagsakYtelseType == FagsakYtelseType.OMSORGSPENGER }
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsDto(norskIdent, FagsakYtelseType.OMSORGSPENGER.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                OmsorgspengerSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s))
            }
        }
    return SvarOmsDto(norskIdent, FagsakYtelseType.OMSORGSPENGER.kode, søknader)
}

internal fun hentUtJournalposter(s: SøknadEntitet) = if (s.journalposter != null) {
    val journalposter = objectMapper().convertValue<JournalposterDto>(s.journalposter)
    journalposter.journalposter.toList()
} else null

internal fun SøknadEntitet.tilPsbvisning(): PleiepengerSøknadDto {
    if (søknad == null) {
        return PleiepengerSøknadDto(soeknadId = this.søknadId, journalposter = hentUtJournalposter(this), harInfoSomIkkeKanPunsjes = false, harMedisinskeOpplysninger = false)
    }
    return objectMapper().convertValue(søknad)
}

internal fun SøknadEntitet.tilOmsvisning(): OmsorgspengerSøknadDto {
    if (søknad == null) {
        return OmsorgspengerSøknadDto(soeknadId = this.søknadId)
    }
    return objectMapper().convertValue(søknad)
}

data class HentPerson(
    val norskIdent: NorskIdentDto,
)

data class PdlResponseDto(
    val person: PdlPersonDto,
)

data class PeriodeDto(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fom: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tom: LocalDate?,
)

data class ArbeidsgiverMedArbeidsforholdId(
    val orgNummerEllerAktørID : String,
    val arbeidsforholdId : List<String>
)

