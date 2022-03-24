package no.nav.k9punsj.domenetjenester.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.Mappe
import no.nav.k9punsj.db.datamodell.SøknadEntitet
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.omsorgspengeraleneomsorg.OmsorgspengerAleneOmsorgSøknadDto
import no.nav.k9punsj.omsorgspengerkronisksyktbarn.OmsorgspengerKroniskSyktBarnSøknadDto
import no.nav.k9punsj.omsorgspengermidlertidigalene.OmsorgspengerMidlertidigAleneSøknadDto
import no.nav.k9punsj.pleiepengerlivetssluttfase.PleiepengerLivetsSluttfaseSøknadDto
import no.nav.k9punsj.pleiepengersyktbarn.PleiepengerSyktBarnSøknadDto
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
    val søknader: List<PleiepengerSyktBarnSøknadDto>?,
)

data class SvarOmsDto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerSøknadDto>?,
)

data class SvarOmsMADto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerMidlertidigAleneSøknadDto>?,
)

data class SvarOmsAODto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<OmsorgspengerAleneOmsorgSøknadDto>?,
)


data class SvarPlsDto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<PleiepengerLivetsSluttfaseSøknadDto>?,
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

internal fun Mappe.tilOmsVisning(norskIdent: NorskIdentDto): SvarOmsDto {
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
                OmsorgspengerSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s))
            }
        }
    return SvarOmsDto(norskIdent, FagsakYtelseType.OMSORGSPENGER.kode, søknader)
}

internal fun Mappe.tilOmsMAVisning(norskIdent: NorskIdentDto): SvarOmsMADto {
    val bunke = hentFor(FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsMADto(norskIdent, FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                OmsorgspengerMidlertidigAleneSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s))
            }
        }
    return SvarOmsMADto(norskIdent, FagsakYtelseType.OMSORGSPENGER_MIDLERTIDIG_ALENE.kode, søknader)
}

internal fun Mappe.tilOmsAOVisning(norskIdent: NorskIdentDto): SvarOmsAODto {
    val bunke = hentFor(FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN)
    if (bunke?.søknader.isNullOrEmpty()) {
        return SvarOmsAODto(norskIdent, FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN.kode, listOf())
    }
    val søknader = bunke?.søknader
        ?.filter { s -> !s.sendtInn }
        ?.map { s ->
            if (s.søknad != null) {
                objectMapper().convertValue(s.søknad)
            } else {
                OmsorgspengerAleneOmsorgSøknadDto(soeknadId = s.søknadId, journalposter = hentUtJournalposter(s))
            }
        }
    return SvarOmsAODto(norskIdent, FagsakYtelseType.OMSORGSPENGER_ALENE_OMSORGEN.kode, søknader)
}

internal fun Mappe.tilPlsVisning(norskIdent: NorskIdentDto): SvarPlsDto {
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

internal fun hentUtJournalposter(s: SøknadEntitet) = if (s.journalposter != null) {
    val journalposter = objectMapper().convertValue<JournalposterDto>(s.journalposter)
    journalposter.journalposter.toList()
} else null

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

internal fun SøknadEntitet.tilOmsvisning(): OmsorgspengerSøknadDto {
    if (søknad == null) {
        return OmsorgspengerSøknadDto(soeknadId = this.søknadId)
    }
    return objectMapper().convertValue(søknad)
}

internal fun SøknadEntitet.tilOmsKSBvisning(): OmsorgspengerKroniskSyktBarnSøknadDto {
    if (søknad == null) {
        return OmsorgspengerKroniskSyktBarnSøknadDto(
            soeknadId = this.søknadId,
            harMedisinskeOpplysninger = false,
            harInfoSomIkkeKanPunsjes = false
        )
    }
    return objectMapper().convertValue(søknad)
}

internal fun SøknadEntitet.tilOmsMAvisning(): OmsorgspengerMidlertidigAleneSøknadDto {
    if (søknad == null) {
        return OmsorgspengerMidlertidigAleneSøknadDto(soeknadId = this.søknadId)
    }
    return objectMapper().convertValue(søknad)
}

internal fun SøknadEntitet.tilOmsAOvisning(): OmsorgspengerAleneOmsorgSøknadDto {
    if (søknad == null) {
        return OmsorgspengerAleneOmsorgSøknadDto(soeknadId = this.søknadId)
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
    val orgNummerEllerAktørID: String,
    val arbeidsforholdId: List<String>
)

