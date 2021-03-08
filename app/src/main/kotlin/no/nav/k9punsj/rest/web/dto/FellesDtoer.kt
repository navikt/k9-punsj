package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.Mappe
import no.nav.k9punsj.db.datamodell.SøknadEntitet
import no.nav.k9punsj.objectMapper
import java.time.LocalDate


typealias PersonIdDto = String
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

data class SvarDto(
    val søker: NorskIdentDto,
    val fagsakTypeKode: String,
    val søknader: List<PleiepengerSøknadVisningDto>?,
)

data class SøknadOppdaterDto<T>(
    val søker: NorskIdentDto,
    val søknadId: SøknadIdDto,
    val søknad: T,
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

internal fun Mappe.tilPsbVisning(norskIdent: NorskIdentDto): SvarDto {
    val bunke = this.bunke.first { b -> b.fagsakYtelseType == FagsakYtelseType.PLEIEPENGER_SYKT_BARN }
    if (bunke.søknader.isNullOrEmpty()) {
        return SvarDto(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, listOf())
    }
    val søknader = bunke.søknader.mapNotNull { s ->
        if (s.søknad != null) {
            objectMapper().convertValue<PleiepengerSøknadVisningDto>(s.søknad)
        } else {
            PleiepengerSøknadVisningDto(soeknadId = s.søknadId, journalposter = if (s.journalposter != null) {
                val any = s.journalposter["journalposter"]!!
                any as List<JournalpostIdDto>
            } else null)
        }
    }
    return SvarDto(norskIdent, FagsakYtelseType.PLEIEPENGER_SYKT_BARN.kode, søknader)
}

internal fun SøknadEntitet.tilPsbvisning(): PleiepengerSøknadVisningDto {
    if (søknad == null) {
       return PleiepengerSøknadVisningDto(soeknadId = this.søknadId, journalposter = if (this.journalposter != null) {
            val any = this.journalposter["journalposter"]!!
            any as List<JournalpostIdDto>
        } else null)
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
    val fom: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tom: LocalDate,
)

