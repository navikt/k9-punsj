package no.nav.k9punsj.db.datamodell

typealias PersonId = String
typealias NorskIdent = String
typealias AktørId = String

data class Person(
    val personId: PersonId,
    val norskIdent: NorskIdent,
    val aktørId: AktørId
)




