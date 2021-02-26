package no.nav.k9punsj.rest.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9punsj.db.datamodell.*
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
    val søknader: List<SøknadDto<PleiepengerSøknadVisningDto>>?,
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

internal inline fun <reified T> Mappe.tilDto(f: (PersonId) -> (NorskIdent)): MappeSvarDTO<T> {
    val bunkerDto = this.bunke.map { bunkeEntitiet -> bunkeEntitiet.tilDto<T>(f) }.toList()
    return MappeSvarDTO(this.mappeId, this.søker.norskIdent, bunkerDto)
}

internal inline fun <reified T> SøknadEntitet.tilDto(f: (PersonId) -> (NorskIdent)): SøknadDto<T> {
    return SøknadDto(
        søknadId = this.søknadId,
        søkerId = f(this.søkerId),
        barnId = if (this.barnId != null) f(this.barnId) else null,
        barnFødselsdato = null,
        søknad = if(this.søknad!= null) objectMapper().convertValue<T>(this.søknad) else null,
        journalposter = if (this.journalposter != null) objectMapper().convertValue(this.journalposter) else null,
        sendtInn = this.sendtInn
    )
}

internal inline fun <reified T>BunkeEntitet.tilDto(f: (PersonId) -> (NorskIdent)): BunkeDto<T> {
    val søknaderDto = this.søknader?.map { søknadEntitet -> søknadEntitet.tilDto<T>(f) }?.toList()
    return BunkeDto(this.bunkeId, this.fagsakYtelseType.kode, søknaderDto)
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
    val tom: LocalDate
)

