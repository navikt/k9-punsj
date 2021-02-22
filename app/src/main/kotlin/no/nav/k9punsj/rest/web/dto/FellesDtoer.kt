package no.nav.k9punsj.rest.web.dto

import no.nav.k9punsj.db.datamodell.*
import no.nav.k9punsj.rest.web.SøknadJson
import java.time.LocalDate


typealias PersonIdDto = String
typealias NorskIdentDto = String
typealias MappeIdDto = String
typealias AktørIdDto = String
typealias BunkeIdDto = String
typealias SøknadIdDto = String
typealias JournalpostIdDto = String

data class PersonDto(
    val personId: PersonIdDto,
    val norskIdent: NorskIdentDto,
    val aktørId: AktørIdDto,
)

data class BunkeDto(
    val bunkeId: BunkeIdDto,
    val fagsakKode: String,
    val søknader: List<SøknadDto<SøknadJson>>?,
)

data class SøknadDto<T>(
    val søknadId: SøknadIdDto,
    val søkerId: NorskIdentDto,
    val barnId: NorskIdentDto? = null,
    val barnFødselsdato: LocalDate? = null,
    val søknad: T?,
    val journalposter: T?,
    val sendt_inn: Boolean? = null,
)

data class JournalposterDto(
    val journalposter: MutableSet<String>
)

internal fun Mappe.tilDto(f: (PersonId) -> (NorskIdent)): MappeSvarDTO {
    val bunkerDto = this.bunke.map { bunkeEntitiet -> bunkeEntitiet.tilDto(f) }.toList()
    return MappeSvarDTO(this.mappeId, this.søker.norskIdent, bunkerDto)
}

internal fun SøknadEntitet.tilDto(f: (PersonId) -> (NorskIdent)): SøknadDto<SøknadJson> {
    return SøknadDto(
        søknadId = this.søknadId,
        søkerId = f(this.søkerId),
        barnId = if (this.barnId != null) f(this.barnId) else null,
        barnFødselsdato = null,
        søknad = this.søknad,
        journalposter = this.journalposter,
        sendt_inn = this.sendt_inn
    )
}

internal fun BunkeEntitet.tilDto(f: (PersonId) -> (NorskIdent)): BunkeDto {
    val søknaderDto = this.søknader?.map { søknadEntitet -> søknadEntitet.tilDto(f) }?.toList()
    return BunkeDto(this.bunkeId, this.fagsakYtelseType.kode, søknaderDto)
}

