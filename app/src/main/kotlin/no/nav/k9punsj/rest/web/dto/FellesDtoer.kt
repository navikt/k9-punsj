package no.nav.k9punsj.rest.web.dto


typealias PersonIdDto = String
typealias NorskIdentDto = String
typealias AktørIdDto = String
typealias JournalpostIdDto = String

data class PersonDtor(
    val personId: PersonIdDto,
    val norskIdent: NorskIdentDto,
    val aktørId: AktørIdDto
)

